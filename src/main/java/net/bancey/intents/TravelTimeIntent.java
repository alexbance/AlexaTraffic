package net.bancey.intents;

import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;
import com.amazon.speech.ui.SsmlOutputSpeech;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.model.*;
import net.bancey.gmaps.GMapsApp;
import org.joda.time.DateTime;

/**
 * AlexaTraffic
 * Created by abance on 28/12/2016.
 */
public class TravelTimeIntent extends AlexaTrafficIntent {

    public TravelTimeIntent(String name) {
        super(name);
    }

    @Override
    public SpeechletResponse handle(String origin, String destination, TravelMode travelMode) {
        GeoApiContext context = new GMapsApp().getContext();
        DistanceMatrixApiRequest request = new DistanceMatrixApiRequest(context)
                .origins(origin + ", GB")
                .destinations(destination + ", GB")
                .mode(travelMode)
                .trafficModel(TrafficModel.PESSIMISTIC)
                .departureTime(new DateTime(System.currentTimeMillis()));

        DistanceMatrix matrix;
        try {
            matrix = request.await();
        } catch (Exception ex) {
            matrix = null;
            ex.printStackTrace();
        }
        String speechText = "Sorry, I couldn't calculate the ETA.";
        String cardText = "Sorry, I couldn't calculate the ETA.";
        if (matrix != null) {
            for (DistanceMatrixRow row : matrix.rows) {
                for(DistanceMatrixElement element: row.elements) {
                    System.out.println(element.status.toString());
                    if(element.status == DistanceMatrixElementStatus.NOT_FOUND || element.status == DistanceMatrixElementStatus.ZERO_RESULTS) {
                        cardText = "One of the locations you provided does not exist. Please try again.";
                        speechText = "One of the locations you provided does not exist. Please try again.";
                        break;
                    }
                    DateTime eta = new DateTime(System.currentTimeMillis());
                    if(travelMode.equals(TravelMode.DRIVING)) {
                        eta = eta.plusSeconds((int)element.durationInTraffic.inSeconds);
                        cardText = "There is " + element.distance + " between " + origin + " and " + destination + ". It will take you approximately " + element.durationInTraffic + " to reach " + destination + " via " + travelMode.toString() + ". Your ETA is " + eta.hourOfDay().getAsText() + ":" + eta.minuteOfHour().getAsText();
                        speechText = "<speak>There is " + element.distance + " between " + origin + " and " + destination + ". It will take you approximately " + element.durationInTraffic + " to reach " + destination + " via " + travelMode.toString() + ". Your ETA is <say-as interpret-as=\"time\">" + eta.hourOfDay().getAsText() + ":" + eta.minuteOfHour().getAsText() + "</say-as></speak>";
                    } else {
                        eta = eta.plusSeconds((int)element.duration.inSeconds);
                        cardText = "There is " + element.distance + " between " + origin + " and " + destination + ". It will take you approximately " + element.duration + " to reach " + destination + " via " + travelMode.toString() + ". Your ETA is " + eta.hourOfDay().getAsText() + ":" + eta.minuteOfHour().getAsText();
                        speechText = "<speak>There is " + element.distance + " between " + origin + " and " + destination + ". It will take you approximately " + element.duration + " to reach " + destination + " via " + travelMode.toString() + ". Your ETA is <say-as interpret-as=\"time\">" + eta.hourOfDay().getAsText() + ":" + eta.minuteOfHour().getAsText() + "</say-as></speak>";
                    }
                }
            }
        }
        SimpleCard card = new SimpleCard();
        card.setTitle("Travel between " + origin + " and " + destination + ".");
        card.setContent(cardText);

        System.out.println(speechText);
        if(speechText.equalsIgnoreCase("One of the locations you provided does not exist. Please try again.")) {
            PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
            Reprompt reprompt = new Reprompt();
            reprompt.setOutputSpeech(speech);
            speech.setText(speechText);
            return SpeechletResponse.newAskResponse(speech, reprompt, card);
        } else {
            SsmlOutputSpeech speech = new SsmlOutputSpeech();
            speech.setSsml(speechText);
            return SpeechletResponse.newTellResponse(speech, card);
        }
    }
}
