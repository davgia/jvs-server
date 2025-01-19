package jvs;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import jvs.stream.Stream;
import jvs.config.ConfigManager;
import jvs.stream.StreamInfo;
import jvs.utils.JsonUtils;
import jvs.utils.Logger;
import jvs.utils.NetworkUtils;
import jvs.utils.Utils;
import jvs.workers.EncodingType;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RESTful implementation of the backend service for the JVS framework.
 */
public class Main extends AbstractVerticle {
	
	/**
	 * Hash map with all current active streams
	 */
	private Map<Integer, Stream> streams = new HashMap<>();

	@Override
	public void start() {

		Logger.info("Starting server...");

		ConfigManager.importConfig(Constants.CONFIG_FILE_PATH);
		if (!ConfigManager.isConfigAvailable()) {
			Logger.error("Configuration file is missing or malformed. Server cannot be started.");
			vertx.close();
			System.exit(-1);
		}

		/* discover local ip if it is not manually defined in the config file */
        if (!NetworkUtils.isValidIPV4Address(ConfigManager.getConfig().getAddress())) {
            Logger.info("User does not define a valid server address, trying to guess it...");
		    String localAddress = NetworkUtils.discoverAddress(true);
		    if (localAddress != null) {
                Logger.info("Automatically discovered the local IP address (" + localAddress + ").");
                ConfigManager.getConfig().setAddress(localAddress);
		    } else {
                Logger.error("Unable to discover a valid local IP address. Server cannot be started.");
                vertx.close();
                System.exit(-2);
            }
        }

        if (restoreStreams()) {
            Logger.info("Streams have been successfully restored.");
        }

        Router router = Router.router(vertx);

		/* get remote paths */
        String remoteAssetsPath = ConfigManager.getConfig().getRemoteAssetsPath();
        String remoteStreamsPath = ConfigManager.getConfig().getRemoteStreamsPath();
        String remoteRootAPIPath = ConfigManager.getConfig().getRemoteRootAPIPath();
        /* get local paths */
        String localAssetsPath = ConfigManager.getConfig().getAssetsPath();
        String localStreamsPath = ConfigManager.getConfig().getStreamsPath();


        /* make sure that all paths are different */
        if (remoteAssetsPath.equals(remoteStreamsPath) && remoteStreamsPath.equals(remoteRootAPIPath) &&
                remoteRootAPIPath.equals(remoteAssetsPath)) {
            Logger.error("Bad configuration: each remote path must be unique.");
            vertx.close();
            System.exit(-3);
        }

        /* make sure that the remote path is not empty */
        if (remoteRootAPIPath.equals("") || remoteAssetsPath.equals("") || remoteStreamsPath.equals("")) {
            Logger.error("Bad configuration: remote path cannot be '/'.");
            vertx.close();
            System.exit(-4);
        }

         /* make sure that the remote path are not absolute */
        if (localAssetsPath.startsWith("/") || localStreamsPath.startsWith("/")) {
            Logger.error("Bad configuration: local path cannot be absolute.");
            vertx.close();
            System.exit(-5);
        }

        /* create body handler */
        router.route().handler(BodyHandler.create());

        /* define route to access web-app assets */
        router.route(remoteAssetsPath + "/*").handler(StaticHandler.create(localAssetsPath));
		/* define route to access encoded manifest */
        router.route(remoteStreamsPath + "/*").handler(StaticHandler.create(localStreamsPath).setCachingEnabled(false));

		/* definitions of the REST API routes */
		router.get(remoteRootAPIPath + "/:streamID").handler(this::handleGetStream);
		router.post(remoteRootAPIPath).handler(this::handleAddStream);
		router.delete(remoteRootAPIPath + "/:streamID").handler(this::handleDeleteStream);
		router.patch(remoteRootAPIPath + "/:streamID").handler(this::handleStopStream);
		router.get(remoteRootAPIPath).handler(this::handleListStreams);

		vertx.createHttpServer().requestHandler(router::accept)
                .listen(ConfigManager.getConfig().getPort(), ConfigManager.getConfig().getAddress());

		Logger.info("Server started (http://" + ConfigManager.getConfig().getAddress() +
                ":" + ConfigManager.getConfig().getPort() + ").");
	}

    @Override
    public void stop() throws Exception {
        super.stop();

        if (streams.size() > 0) {
            Logger.info("Stopping all workers...");
            for (Stream s : streams.values()) {
                s.stopEncoding();
            }

            if (saveStreams()) {
                Logger.info("Streams have been successfully saved.");
            } else {
                Logger.info("Failed to save streams.");
            }
        }
    }

	/* REQUEST HANDLERS */

    /**
     * Handles the get stream information request.
     * @param routingContext The routing context.
     */
	private void handleGetStream(RoutingContext routingContext) {

		HttpServerResponse response = routingContext.response();
		String requestedID = routingContext.request().getParam("streamID");
		int streamID = -1;

        //get client address
        String host = routingContext.request().remoteAddress().host();
        Logger.info("Client (ip: " + host + "): requests information about stream with id: " + requestedID + ".");

		if (!requestedID.isEmpty()) {
			try {
				streamID = Integer.parseInt(requestedID);
			} catch (NumberFormatException ex) {
				Logger.error("Unable to parse integer from GET request (value: " + requestedID + ").");
				ex.printStackTrace();
			}
		}
	
		if (streamID > 0) {
			Stream stream = streams.get(streamID);
			if (stream == null) {
				sendError(404, "Unable to find the stream.", response);
			} else {
				response.setStatusCode(200)
				.setStatusMessage("OK")
				.putHeader("Access-Control-Allow-Origin", "*")
				.putHeader("content-type", "application/json")
				.end(stream.toJSON().encode());
			}
		} else {
			sendError(400, "Missing or invalid stream id.", response);
		}
	}

    /**
     * Handles the add new stream request.
     * @param routingContext The routing context.
     */
	private void handleAddStream(RoutingContext routingContext) {

		HttpServerResponse response = routingContext.response();

        //get client address
        String host = routingContext.request().remoteAddress().host();
        Logger.info("Client (ip: " + host + "): requests to add a new stream.");

		JsonObject json = null;

		try {
			json = routingContext.getBodyAsJson();
		} catch (Exception ex) {
            Logger.error("Unable to parse json body from the incoming request (value: " + routingContext.getBodyAsString() + ").");
            ex.printStackTrace();
		}

        if (json == null || !addStream(json, response)) {
            sendError(400, "Unable to initialize the new stream and start the encoding.", response);
        }
	}

    /**
     * Handles the delete stream request.
     * @param routingContext The routing context.
     */
	private void handleDeleteStream(RoutingContext routingContext) {

		HttpServerResponse response = routingContext.response();
		String requestedID = routingContext.request().getParam("streamID");
		int streamID = -1;

        //get client address
        String host = routingContext.request().remoteAddress().host();
        Logger.info("Client (ip: " + host + "): requests removal of stream with id: " + requestedID);
		
		if (!requestedID.isEmpty()) {
			try {
				streamID = Integer.parseInt(requestedID);
			} catch (NumberFormatException ex) {
				Logger.error("Unable to parse integer from DELETE request (value: " + requestedID + ").");
				ex.printStackTrace();		
			}
		}
		
		if (deleteStream(streamID)) {		
			sendSuccess(response);
		} else {
			sendError(400, "Unable to delete stream.", response);
		}
	}

    /**
     * Handles the list stream request.
     * @param routingContext The routing context.
     */
	private void handleListStreams(RoutingContext routingContext) {

        //get client address
        String host = routingContext.request().remoteAddress().host();
        Logger.info("Client (ip: " + host + "): requests all active streams.");

		JsonArray arr = new JsonArray();
		streams.forEach((k, v) -> arr.add(v.toJSON()));
		
		routingContext.response()
		.setStatusCode(200)
		.setStatusMessage("OK")
		.putHeader("Access-Control-Allow-Origin", "*")
		.putHeader("content-type", "application/json")
		.end(arr.encode());
	}

    /**
     * Handles the stop stream request.
     * @param routingContext The routing context.
     */
	private void handleStopStream(RoutingContext routingContext) {
        HttpServerResponse response = routingContext.response();
        String requestedID = routingContext.request().getParam("streamID");
        int streamID = -1;

        //get client address
        String host = routingContext.request().remoteAddress().host();
        Logger.info("Client (ip: " + host + "): requests stopping of stream with id: " + requestedID);

        if (!requestedID.isEmpty()) {
            try {
                streamID = Integer.parseInt(requestedID);
            } catch (NumberFormatException ex) {
                Logger.error("Unable to parse integer from PATCH request (value: " + requestedID + ").");
                ex.printStackTrace();
            }
        }

        if (stopStream(streamID)) {
            sendSuccess(response);
        } else {
            sendError(400, "Unable to stop stream.", response);
        }
    }

    /**
     * Sends a message to signal client the success of an operation.
     * @param response The response used to send the reply.
     */
	private void sendSuccess(HttpServerResponse response) {
        response.setStatusCode(200)
                .setStatusMessage("OK")
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("status", "0").put("message", "OK").encode());
    }

    /**
     * Sends a message to signal client of the failure of an operation.
     * @param statusCode The HTTP status to report.
     * @param message The message to report inside the json of the body.
     * @param response The response used to send the reply.
     */
	private void sendError(int statusCode, String message, HttpServerResponse response) {
		Logger.error("Server has encoutered an error, sending error code: " + statusCode);
		response.setStatusCode(statusCode)
                .putHeader("Access-Control-Allow-Origin", "*")
                .putHeader("content-type", "application/json")
                .end(new JsonObject().put("status", "-1").put("message", message).encode());
	}

	/* PROTECTED METHODS */

    /**
     * Adds a new stream and start the encoding.
     *
     * @param json The json from which parse stream information.
     * @param response The httpServerResponse used to send the response to the client if the encode starts correctly.
     * @return True, if the stream has been successfully initialized and the encode has been started; otherwise, false.
     */
	private synchronized Boolean addStream(final JsonObject json, final HttpServerResponse response) {
		EncodingType type = EncodingType.MPEG_DASH_H264_AAC; //default
        RTSPMode mode = RTSPMode.CLIENT; //default
        List<String> extraArgs = null; //default

        //clean streams collection
        cleanStreams();

        //safely retrieve encoding type
		if (json.containsKey(Constants.ENCODINGTYPE_KEY)) {
			int parsedType = json.getInteger(Constants.ENCODINGTYPE_KEY, 0);

			if (parsedType < EncodingType.values().length) {
				type = EncodingType.values()[parsedType];
			}
		}

        //safely retrieve ffmpeg mode
        if (json.containsKey(Constants.MODE_KEY)) {
            int parsedMode = json.getInteger(Constants.MODE_KEY, 0);

            if (parsedMode < RTSPMode.values().length) {
                mode = RTSPMode.values()[parsedMode];
            }
        }

        //safely retrieve custom arguments for ffmpeg
        if (json.containsKey(Constants.CUSTOM_ARGS_KEY)) {
		    try {
                JsonArray parsedArgs = json.getJsonArray(Constants.CUSTOM_ARGS_KEY);

                if (parsedArgs != null && parsedArgs.size() > 0) {
                    extraArgs = new ArrayList<>();
                    for (Object arg : parsedArgs.getList()) {
                        extraArgs.add((String)arg);
                    }
                }
            } catch (Exception ex) {
                Logger.warn("Extra client ffmpeg arguments cannot be parsed. Will be ignored.");
            }
        }

        //check whether all necessary informations are present to setup ffmpeg as rtsp server
        if (mode == RTSPMode.SERVER && (ConfigManager.getConfig().getAnnouncePath() == null ||
                        ConfigManager.getConfig().getListeningPort() <= 0)) {
            Logger.error("Missing announce path or listening port, ffmpeg cannot be configured as rtsp server.");
            return false;
        }

		int newID = computeNewID();
		Stream stream;

		try {
			stream = new Stream(json);
		} catch (InvalidParameterException ex) {
		    Logger.error(ex.getLocalizedMessage());
			return false;
		}

		stream.setCreationDate(new Date());
		stream.setDirectory(ConfigManager.getConfig().getStreamsPath() + File.separator + "stream_" + newID);
		stream.setID(newID);
		streams.put(newID, stream);

        //prepare the encoder
        stream.prepareEncoder(type, mode);

        //add extra arguments if needed
        if (extraArgs != null) {
            stream.setExtraArguments(extraArgs);
        }

        //parse client information about streams if the encoding type is not passthrough
        if (mode == RTSPMode.SERVER && type != EncodingType.MPEG_DASH_PASSTHROUGH) {

            JsonObject parsedInfo = json.getJsonObject(Constants.INFOS_KEY);
            String clientInfo = parsedInfo.encode();

            if (JsonUtils.isJSONValid(clientInfo)) {
                StreamInfo streamInfo = StreamInfo.parseStreamInfo(clientInfo);

                if (streamInfo != null) {
                    stream.setUserStreamInfo(streamInfo);
                } else {
                    Logger.error("Parsing of json stream information failed, ffmpeg cannot be configured as rtsp server.");
                    stream.setError();
                    return false;
                }
            } else {
                Logger.error("Missing or invalid stream information, ffmpeg cannot be configured as rtsp server.");
                stream.setError();
                return false;
            }
        }

		//start encoding
        if (stream.startEncoding()) {
            //if ffmpeg is configured like server mode, put in the response additional information about announce
            if (mode == RTSPMode.SERVER) {
                response.setStatusCode(200)
                        .setStatusMessage("OK")
                        .putHeader("Access-Control-Allow-Origin", "*")
                        .putHeader("content-type", "application/json")
                        .end(new JsonObject().put("status", "0").put("message", "OK")
                                .put(Constants.LISTENINGPORT_KEY, ConfigManager.getConfig().getListeningPort())
                                .put(Constants.ANNOUNCEPATH_KEY, ConfigManager.getConfig().getAnnouncePath() +
                                        "/stream" + newID)
                                .put(Constants.ID_KEY, stream.getID()).encode());
            } else {
                sendSuccess(response);
            }
            return true;
        }

        stream.setError();
        return false;
    }

    /**
     * Stops the stream encoding.
     *
     * @param id The id of the target stream.
     * @return True, if the ffmpeg process is successfully stopped; otherwise false.
     */
    private synchronized Boolean stopStream(final int id) {
        if (streams.containsKey(id)) {
            try {
                //gracefully stops encoding if the selected mode is RTSP server
                streams.get(id).stopEncodingGracefully();
                return true;
            } catch (Exception ex) {
                Logger.warn("Unable to stop stream with id: " + id + ". " + ex.getLocalizedMessage());
                return false;
            }
        } else {
            Logger.warn("Unable to stop stream with id: " + id + ". The stream does not exists.");
            return false;
        }
    }

    /**
     * Deletes the stream with the specified id.
     * @param id The id of the stream to delete.
     * @return True, if the stream is successfully removed; otherwise false.
     */
	private synchronized Boolean deleteStream(final int id) {
		if (streams.containsKey(id)) {
		    try {
                streams.get(id).stopEncoding(); //stop encoding
                deleteDirectory(new File(streams.get(id).getDirectory()));
                streams.remove(id);
                return true;
            } catch (Exception ex) {
                Logger.warn("Unable to delete stream with id: " + id + ". " + ex.getLocalizedMessage());
                return false;
            }
		} else {
			Logger.warn("Unable to delete stream with id: " + id + ". The stream does not exists.");
			return false;
		}
	}

	/* HELPER METHODS */

    /**
     * Cleans global streams collections from instances that had errors.
     */
	private void cleanStreams() {
        if (streams != null && streams.size() > 0) {
            try {
                Logger.info("Cleaning streams...");
                List<Integer> errorStreams = streams.entrySet().stream()
                        .filter(i -> i.getValue().hadErrors())
                        .map(i -> i.getValue().getID())
                        .collect(Collectors.toList());

                for (Integer id : errorStreams) {
                    deleteStream(id);
                }
                Logger.info("Cleaning completed.");
            } catch (Exception e) {
                Logger.warn("Unable to clean streams.");
                e.printStackTrace();
            }
        }
    }

    /**
     * Computes a new unique stream id.
     * @return The new id, -1 in case of failure to generate a new unique id.
     */
	private int computeNewID() {
		int newID = -1;
		
		if (streams.size() > 0) {
			Optional<Integer> max = streams.keySet().stream().max(Comparator.naturalOrder());
			try {
				newID = max.get() + 1;	
			} catch (NoSuchElementException e) {
				Logger.error("Unable to find the maximum id inside the streams collection.");
				e.printStackTrace();
			}
		} else {
			newID = 1;
		}
		return newID;
	}

    /**
     * Restores streams from a backup file, if it's present
     * @return True, if the operation is completed without errors; otherwise false.
     */
	private boolean restoreStreams() {

	    if (Files.notExists(Paths.get(Constants.STREAMS_FILE_PATH))) {
            return false;
        }

        JsonObject jsonObject = JsonUtils.readJsonFromFile(Constants.STREAMS_FILE_PATH);

        if (jsonObject != null) {
            try {
                //deserialize
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.registerModule(new JavaTimeModule());
                Map<Integer, Stream> parsedStreams = objectMapper.readValue(jsonObject.encode(), new TypeReference<Map<Integer, Stream>>(){});

                //add entries if the manifest exists
                for (Map.Entry<Integer, Stream> entry : parsedStreams.entrySet()) {
                    String manifestPath = entry.getValue().getDirectory() + File.separator + "manifest.mpd";
                    if (Files.exists(Paths.get(manifestPath))) {
                        streams.put(entry.getKey(), entry.getValue());
                    }
                }

                //delete unused folders
                File[] directories = new File(ConfigManager.getConfig().getStreamsPath()).listFiles(File::isDirectory);

                if (directories != null) {

                    List<Path> usedDirectories = streams.values().stream()
                            .map(i -> Paths.get(i.getDirectory()).normalize())
                            .collect(Collectors.toList());

                    for (File file : directories) {
                        Path currPath = Paths.get(file.getPath()).normalize();
                        if (!usedDirectories.contains(currPath)) {
                            deleteDirectory(file);
                        }
                    }
                }
            } catch (Exception ex) {
                Logger.error("Unable to restore streams:");
                ex.printStackTrace();
            }
        } else {
            Logger.warn("Unable to restore streams, the file may be corrupted.");
            return false;
        }

        return true;
    }

    /**
     * Saves all current active streams to file.
     * @return True, if the operations is completed without errors; otherwise false.
     */
	private boolean saveStreams() {
        try{
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String serialized = objectMapper.writeValueAsString(streams);

            FileWriter fstream = new FileWriter(Constants.STREAMS_FILE_PATH);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(serialized);
            out.close();
        }catch (Exception e){
            Logger.error("Unable to serialize streams: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Deletes a folder and all its content (recursively).
     * @param file The target folder to delete.
     */
    private void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDirectory(f);
            }
        }
        file.delete();
    }
}