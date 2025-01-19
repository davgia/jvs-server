package jvs.workers;

import jvs.utils.DurationUtils;
import jvs.utils.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.time.Duration;
import java.util.List;

/**
 * Worker used to update dash manifest once conversion is finished from dynamic to static.
 */
public class MPDUpdaterWorker extends Worker {

    /**
     * Final duration of the stream.
     */
    private Duration duration;

    /**
     * FFProbeWorker constructor
     *
     * @param commands The commands to execute in background.
     * @param duration The final duration of the stream.
     */
    protected MPDUpdaterWorker(final List<String> commands, final Duration duration) {
        super(commands);
        this.duration = duration;
    }

    /**
     * Defines the thread operations.
     */
    @Override
    public void run() {
        try {
            if (commands.size() == 2 || !duration.isZero() || !duration.isNegative()) {
                String manifestPath = commands.get(0) + File.separator + commands.get(1);
                String finalDuration = DurationUtils.formatToISO8601(duration);

                //open the xml to read and edit
                File manifestFile = new File(manifestPath);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(manifestFile);

                doc.getDocumentElement().normalize();
                NodeList nList = doc.getElementsByTagName("MPD");

                Element rootNode = (Element)nList.item(0);

                rootNode.setAttribute("type", "static");
                rootNode.setAttribute("mediaPresentationDuration", finalDuration);
                rootNode.removeAttribute("minimumUpdatePeriod");
                rootNode.removeAttribute("availabilityStartTime");
                rootNode.removeAttribute("timeShiftBufferDepth");

                // write the content into xml file
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(manifestFile);
                transformer.transform(source, result);

                Logger.info("Dash manifest has been successfully update from dynamic to static.");
            } else {
                Logger.error("Unable to update dash manifest if the mpd file path or the final duration of the stream are not valid.");
            }

        } catch (Exception e) {
            Logger.error("Unable to update WEBM DASH manifest, error: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }
}
