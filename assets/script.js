/* Change server address according to JVS Server configuration */
var server_address = 'http://127.0.0.1:8081/';
var refresh_time = 10000;

/**
 * jQuery document ready function: load data and attach events to DOM elements.
 */
$(document).ready(function () {

	// Install built-in polyfills to patch browser incompatibilities.
	shaka.polyfill.installAll();

	//load data
	loadData();

	$("#btnRefresh").click(function () {
		console.log("Refreshing...");
		loadData();
		console.log("Refresh completed.");
	});
	
	$("#alertError").hide();
	$(".outer-container").hide();
	$(".inner-container").hide();
							
	$(".button-close").click(function () {
		$(".outer-container").hide();
		$(".inner-container").hide();
		unloadPlayer();
	});

	$(".close").click(function(){
		$("#alertError").alert("close");
	});

    //refresh data each 10 seconds
	setInterval(function(){
        loadData();
    }, refresh_time);
});

/**
 * Requests, parses and renders new data from the JVS server.
 */
function loadData() {
	console.log("Loading data...");

	//clear panel
	$("#content").children().remove();
	
	//request list of streams
	$.getJSON(server_address + "streams", function (data) {
		console.log("Server replied with the list of all available streams.");

		//render all streams
		$.each(data, function (key, val) {		
			var humanizedDuration = moment.duration(val.duration).humanize();

            var date = new Date(val.creationDate);

			$("<tr><td>" + val.id + "</td>" +
			    "<td>" + val.title + "</td>" +
			    "<td>" + val.descr + " (" + date.toDateString() + " / " + val.streamType + ")</td>" +
			    "<td>" + humanizedDuration + "</td>" +
			    "<td>" + (val.isLive ? "Yes" : "No") + "</td>" +
			    "<td>" +
				"<button class='btn btn-info btn-sm button-play' data-id='" + val.id + "'>" +
				"   <span class='glyphicon glyphicon-play'></span>" +
				"</button>" +
				"</td>" +
				"</tr>").appendTo("#content");
		});

        attachEvents();

		console.log("Data successfully loaded.");
	});
}

/**
 * Attach onClick event to play button
 */
function attachEvents() {
    //attach on click event for the play button
	$(".button-play").click(function () {
        var streamID = $(this).attr('data-id');
    	console.log("Try to start playback of stream with id: " + streamID);

    	//request selected stream informations
    	$.getJSON(server_address + "streams/" + streamID, function (data) {
    		console.log("Server response with info about stream with id: " + streamID);
    		console.log("Target manifest: " + data.manifest);

    		// Check to see if the browser supports the basic APIs Shaka needs.
    		if (!shaka.Player.isBrowserSupported()) {
    			// This browser does not have the minimum set of APIs we need.
    			alertError('This browser is not supported!');
    			return;
    		}

            //get video element from Dom
    		var video = document.getElementById("videoPlayer");
    		//init shaka player with the video element
    		var player = new shaka.Player(video);

    		// Attach player to the window to make it easy to access in the JS console.
    		window.player = player;

    		// Listen for error events
    		player.addEventListener('error', onErrorEvent);
    		//declare common synchronization server
    		player.configure({ clockSyncUri: "http://time.akamai.com/?iso" });
    		// Try to load a manifest asynchronously
    		player.load(data.manifest).then(function () {
    			$(".outer-container").show();
    			$(".inner-container").show();
    			console.log('The video has now been loaded!');
    			video.play();
    		}).catch (onError); // onError is executed if the asynchronous load fails.
    	});
    });
}

/**
 * Unload player attached to the video element.
 */
function unloadPlayer() {
    if (window.player) {
        window.player.unload();
    }
}

/**
 * Handles on error even of Shaka player.
 * @param {event} event The event object
 */
function onErrorEvent(event) {
    onError(event.detail);

}

/**
 * Print the error to console and call showAlert function.
 * @param {error} error The error object
 */
function onError(error) {
	//showAlert("Shaka player reported an error with code: " + error.code);
	console.error("Shaka player error: " + error.code);
	unloadPlayer();
}

/**
 * Show a new bootstram error alert with the input message.
 * @param {string} message The message to show.
 */
function showAlert(message) {
	$("#alertText").text(message);
	$("#alertError").fadeTo(2000, 500).slideUp(500, function() {
		$("#alertError").slideUp(500);
	});  
}