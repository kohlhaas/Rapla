package org.rapla.plugin.wwi2021;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import org.jboss.resteasy.annotations.jaxrs.FormParam;
import org.rapla.RaplaSystemInfo;
import org.rapla.entities.Entity;
import org.rapla.entities.EntityNotFoundException;
import org.rapla.entities.User;
import org.rapla.entities.domain.Appointment;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.storage.ReferenceInfo;
import org.rapla.facade.RaplaFacade;
import org.rapla.framework.RaplaException;
import org.rapla.logger.Logger;
import org.rapla.server.RemoteSession;
import org.rapla.server.internal.ServerContainerContext;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.HttpsURLConnection;
import javax.print.attribute.standard.Media;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.HttpURLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.rapla.storage.RaplaSecurityException;


@Singleton
@Path("semesterplan")
public class ImportController {
    @Inject
    public RaplaFacade facade;

    @Inject
    public Logger logger;

    @Inject
    RemoteSession session;
    private final HttpServletRequest request;

    @Inject
    public ImportController(@Context HttpServletRequest request){
        this.request = request;
    }

    @POST
    @Path("/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public void importSemesterplan(@Context HttpServletRequest req, @Context HttpServletResponse res, @MultipartForm ICSFileUploadForm form) throws Exception {
        InputStream icsInputStream = null;
        User user;

        try {
            // Check and get the user from the session
            user = session.checkAndGetUser(req);
        } catch (RaplaSecurityException sec) {
            logger.error("Unauthorized access: No user found in session.", sec);
            res.setStatus(HttpsURLConnection.HTTP_UNAUTHORIZED);
            return;
        }

        try {
            // Get the ICS file input stream
            icsInputStream = form.getIcsFile();
            String ics = convertStreamToString(icsInputStream);
            //String ics = new String(icsInputStream.readAllBytes());
            //String ics = "";
            Map<ReferenceInfo<Reservation>, List<Appointment>> result = importAppointmentsFromIcs(ics);
            List<Reservation> reservationsToStore = new ArrayList<>();

            // Process the ICS file and update reservations
            for (Map.Entry<ReferenceInfo<Reservation>, List<Appointment>> entry : result.entrySet()) {
                Reservation reservation;
                ReferenceInfo<Reservation> reservationId = entry.getKey();
                List<Appointment> appointments = entry.getValue();

                try {
                    reservation = facade.edit(facade.resolve(reservationId));
                } catch(EntityNotFoundException e) {
                    logger.error("Module id not found", e);
                    continue;
                }

                List<Appointment> placeholderAppointments = Arrays.asList(reservation.getAppointments());
                for (Appointment appointment : placeholderAppointments) {
                    reservation.removeAppointment(appointment);
                }

                for (Appointment appointment : appointments) {
                    reservation.addAppointment(appointment);
                }

                reservationsToStore.add(reservation);
                logger.info("Successfully added reservation appointments for id " + reservationId + " from imported ics-File");
            }

            // Store all reservations at once
            Entity[] events = reservationsToStore.toArray(new Reservation[0]);
            //facade.storeObjects(events);
            facade.storeAndRemove(events, new Entity[]{}, user);


            res.setStatus(HttpsURLConnection.HTTP_OK);

        } catch (RaplaSecurityException e) {
            logger.error("User doesn´t have enough rights for storing the ICS file", e);
            res.setStatus(HttpsURLConnection.HTTP_FORBIDDEN);
        } catch (Exception e) {
            logger.error("Error processing the ICS file", e);
            res.setStatus(HttpsURLConnection.HTTP_INTERNAL_ERROR);
        } finally {
            if (icsInputStream != null) {
                icsInputStream.close();
            }
            generatePage(res, res.getStatus());
        }
    }


    public Map<ReferenceInfo<Reservation>, List<Appointment>> importAppointmentsFromIcs(String icsFile) throws RaplaException, ParseException, ParserException, IOException {

        Map<ReferenceInfo<Reservation>, List<Appointment>> newMap = new LinkedHashMap<>();

        // Use an ICS parser to parse the ICS file string
        StringReader sin = new StringReader(icsFile);
        CalendarBuilder builder = new CalendarBuilder();
        net.fortuna.ical4j.model.Calendar calendar = builder.build(sin);

        // Temporary map to group appointments by X-RAPLA-ID
        Map<String, List<Appointment>> tempMap = new HashMap<>();

        for (Component event : calendar.getComponents(Component.VEVENT)) {
            // Extract DTSTART, DTEND, and X-RAPLA-ID
            String start = event.getProperty(Property.DTSTART).getValue();
            String end = event.getProperty(Property.DTEND).getValue();
            String raplaId = event.getProperty("X-RAPLA-ID").getValue();

            // Convert start and end strings to Date objects
            Date startDate = convertToDate(start);
            Date endDate = convertToDate(end);

            // Create a new appointment with the start and end dates
            Appointment appointment = facade.newAppointmentWithUser(startDate, endDate, facade.getUser("admin"));

            // Group appointments by X-RAPLA-ID
            tempMap.computeIfAbsent(raplaId, k -> new ArrayList<>()).add(appointment);
        }

        // Convert grouped appointments into the final map format
        for (Map.Entry<String, List<Appointment>> entry : tempMap.entrySet()) {
            String raplaId = entry.getKey();
            List<Appointment> appointments = entry.getValue();

            // Convert raplaId to ReferenceInfo<Reservation>
            ReferenceInfo<Reservation> refInfo = new ReferenceInfo<>(raplaId, Reservation.class);

            // Put the grouped appointments in the newMap
            newMap.put(refInfo, appointments);
        }
        return newMap;
    }

    private Date convertToDate(String timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            return sdf.parse(timestamp);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void generatePage(HttpServletResponse res, int responseCode) throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        PrintWriter out = res.getWriter();

        String title, heading, message, color;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            title = "Import Erfolg";
            heading = "Erfolgreich hinzugefügt";
            message = "Der Semesterplan wurde erfolgreich importiert";
            color = "#4CAF50"; // Green color for success
        } else if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            title = "Zugriff verweigert";
            heading = "Nicht autorisiert";
            message = "Sie sind nicht berechtigt, diese Aktion auszuführen.";
            color = "#f44336"; // Red color for error
        } else {
            title = "Import fehlerhaft";
            heading = "Fehler erkannt - Fehlercode: " + responseCode;
            message = "Beim Import des Semesterplans ist ein Fehler aufgetreten.";
            color = "#4a90e2"; // Blue color for error
        }

        out.println("<html>");
        out.println("<head>");
        out.println("  <title>" + title + "</title>");
        out.println("  <style>");
        out.println("    body { font-family: Arial, sans-serif; background-color: #f4f4f9; padding: 20px; color: #7c898f; }");
        out.println("    .container { max-width: 600px; margin: auto; background: white; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0,0,0,0.1); text-align: center; }");
        out.println("    h1 { color: " + color + "; }");
        out.println("    .button { background-color: " + color + "; color: white; padding: 10px 20px; border: none; border-radius: 4px; cursor: pointer; font-weight: bold; margin-top: 20px; text-decoration: none; display: inline-block; transition: background-color 0.3s ease; }");
        out.println("    .button:hover { background-color: #357ABD; }"); // Hover color to a darker blue
        out.println("  </style>");
        out.println("</head>");
        out.println("<body>");
        out.println("  <div class='container'>");
        out.println("    <h1>" + heading + "</h1>");
        out.println("    <p>" + message + "</p>");
        out.println("    <a href='/rapla/semesterplan' class='button'>Zurück zur Startseite</a>");
        out.println("  </div>");
        out.println("</body>");
        out.println("</html>");

        out.close();
    }

    private String convertStreamToString(InputStream is) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }


}
