/**
 *
 */
package org.rapla.plugin.wwi2021;

import org.rapla.RaplaSystemInfo;
import org.rapla.entities.Entity;
import org.rapla.entities.domain.Allocatable;
import org.rapla.entities.domain.Reservation;
import org.rapla.entities.dynamictype.ClassificationFilter;
import org.rapla.entities.dynamictype.DynamicType;
import org.rapla.entities.dynamictype.Classification;
import org.rapla.entities.dynamictype.Attribute;
import org.rapla.entities.storage.ReferenceInfo;
import org.rapla.facade.RaplaFacade;
import org.rapla.framework.RaplaException;
import org.rapla.logger.Logger;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.sync.SynchronizedCompletablePromise;
import org.rapla.server.internal.RaplaStatusEntry;
import org.rapla.server.internal.ServerContainerContext;

import microsoft.exchange.webservices.data.core.service.item.Appointment;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;

import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.stream.Collectors;



@Singleton
@Path("pruefungen")
public class RaplaPruefungen {
    @Inject
    public RaplaFacade facade;

    @Inject
    public Logger logger;
    @Inject RaplaSystemInfo m_i18n;
    @Inject ServerContainerContext serverContainerContext;
    @Inject
    public RaplaPruefungen()
    {
    }

    @GET
    @Path("kurs")
    public void generateKurs( @Context HttpServletRequest request, @Context HttpServletResponse response ) throws Exception {
        java.io.PrintWriter out = response.getWriter();
        String kursId = request.getParameter("id");

        ReferenceInfo<Allocatable> kurs =  new ReferenceInfo<>(kursId, Allocatable.class);
        Allocatable resolve = facade.resolve(kurs);

        String kursName = resolve.getName(null);

        DynamicType pruefung = facade.getDynamicType("Pruefung");
        ClassificationFilter[] pruefungen = pruefung.newClassificationFilter().toArray();
        Promise<Collection<Reservation>> allePruefungen = facade.getReservationsForAllocatable(new Allocatable[] {resolve}, null, null, pruefungen);
        Collection<Reservation> reservations = SynchronizedCompletablePromise.waitFor(allePruefungen, 10000,logger);
        // String linkPrefix = request.getPathTranslated() != null ? "../" : "";


        out.println( "<html>" );
        out.println( "<head>" );
        out.println("<title>Prüfungsverzeichnis: " + kursName + "</title>"); 
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        // out.println("<link REL=\"stylesheet\" href=\"" + linkPrefix + "pruefungsansicht.css\" type=\"text/css\">");
        // out.println("<link REL=\"stylesheet\" href=\"pruefungsansicht.css\" type=\"text/css\">");
        out.println("<style>");
        out.println("body {\r\n" + //
                        "    font-family: Arial, sans-serif;\r\n" + //
                        "    margin: 0;\r\n" + //
                        "    padding: 0;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        ".container {\r\n" + //
                        "    padding-top: 10px;\r\n" + //
                        "    width: 90%;\r\n" + //
                        "    margin: auto;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        ".card,\r\n" + //
                        ".table-container {\r\n" + //
                        "    padding: 10px;\r\n" + //
                        "    margin-bottom: 10px;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "header h1,\r\n" + //
                        ".vorlesungen-header h2 {\r\n" + //
                        "    margin: 0;\r\n" + //
                        "    padding: 10px 0;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        ".vorlesungen-header {\r\n" + //
                        "    border-bottom: 1px solid #ccc;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        ".grid {\r\n" + //
                        "    display: grid;\r\n" + //
                        "    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));\r\n" + //
                        "    gap: 10px;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        ".card {\r\n" + //
                        "    padding: 15px;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "table {\r\n" + //
                        "    width: 100%;\r\n" + //
                        "    border-collapse: collapse;\r\n" + //
                        "    margin-bottom: 10px;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "th,\r\n" + //
                        "td {\r\n" + //
                        "    border: 1px solid #ccc;\r\n" + //
                        "    padding: 8px;\r\n" + //
                        "    text-align: left;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "th {\r\n" + //
                        "    background-color: #f2f2f2;\r\n" + //
                        "}\r\n" + //
                        "\r\n" + //
                        "@media (min-width: 900px) {\r\n" + //
                        "    .grid {\r\n" + //
                        "        grid-template-columns: repeat(3, 1fr);\r\n" + //
                        "    }\r\n" + //
                        "}");
        out.println("</style>");

        out.println("</head>" );

        out.println( "<body>" );
        out.println("<div class=\"container\">");
        out.println("<header> <h1>Prüfungsverzeichnis - Kurs " + kursName + "</h1></header>");

        out.println("<div class=\"filter-semester-container\">");
        // TODO: Semesterfilter einbauen
        out.println("<h2>Semester: ");
            out.println("<select name=\"semester\" id=\"dropdown_semester\">");
                    out.println("<option value=\"1\">1</option>");
                    out.println("<option value=\"2\">2</option>");
                    out.println("<option value=\"3\">3</option>");
                    out.println("<option value=\"4\">4</option>");
                    out.println("<option value=\"5\">5</option>");
                    out.println("<option value=\"6\">6</option>");
            out.println("</select>");
        out.println("</h2>");
        out.println("</div>");      // filter-semester-container
                
        // - - -  View lectures:
        out.println("<div class=\"lectures-container\">");
        out.println("<div class=\"container-header\"> <h2>Vorlesungen</h2> </div>");      
        out.println("<div class=\"grid\" id=\"lectures-grid\">");
        
        // Generate cards for each lecture and exam performance
        for (Reservation reservation:reservations) {
            // TODO: Error handling für leere Werte

            out.println("<div class=\"card\">");
            out.println("<h4><b>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Name")) + " - " + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("unit_name")) + "</b></h4>");
            out.println("<table>");
            out.println("<tr><th>Prüfungsart</th><td>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Pruefungsart")) + "</td></tr>");
            out.println("<tr><th>Prüfungsdetails</th><td>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Beschreibung")) + "</td></tr>");
            
            // TODO: Termine richtig anzeigen (Formatieren + je Prüfung)
            out.println("<tr><th>Termine</th><td>" + reservation.getSortedAppointments()+ "</td></tr>");
            // for (Appointment appointment:reservation.getAppointments()) {
            //     out.println(appointment.getStart());
            // }
            if (reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("max_punkte")) != null) {
                out.println("<tr><th>Max. Punkte</th><td>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("max_punkte")) + "</td></tr>");            
            } else {
                out.println("<tr><th>Max. Punkte</th><td></td></tr>");
            }
            String dozierende = "";
            for (Allocatable resource:reservation.getPersons()) {
                dozierende += resource.getName(null) + "; ";
            }
            out.println("<tr><th>Dozierende</th><td>" + dozierende + "</td></tr>");
            if (reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("link")) != null){
                out.println("<tr><th>Moodle</th><td><p><a href=\"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("link")) + "\">Link</a></p></td></tr>");
            } else {
                out.println("<tr><th>Moodle</th><td></td></tr>");
            }
            out.println("</table>");
            out.println("</div>");    // card
        }
        
        out.println("</div>");      // grid
        out.println("</div>");      // lectures-container
        

        // - - -  View exams:
        out.println("<div class=\"table-container\">");
        out.println("<div class=\"container-header\"> <h2>Prüfungen</h2> </div>");
        out.println("<table id=\"exams-table\">");
        out.println("<tr><th>Datum</th><th>Uhrzeit</th><th>Raum</th><th>Modul</th><th>Dauer</th><th>Vorlesung / Modul-(teil)klausur</th><th>Klausuranteil</th></tr>");

        // Preprocess reservations to group by unit_name
        Map<String, List<Reservation>> groupedByUnitName = reservations.stream()
            .filter(reservation -> reservation.getClassification().getValueAsString(reservation.getClassification().getAttribute("Pruefungsart"), null).equalsIgnoreCase("Klausur"))
            .collect(Collectors.groupingBy(reservation -> reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("unit_name")).toString()));

        // Generate table row for each unit_name
        for (Map.Entry<String, List<Reservation>> entry : groupedByUnitName.entrySet()) {
            String unitName = entry.getKey();
            Integer unitPoints = 120;
            List<Reservation> unitReservations = entry.getValue();

            // Initialize variables for the combined exam details
            String examDay = null;
            Date earliestTime = null;
            String room = null;
            int totalDuration = 0;
            StringBuilder names = new StringBuilder();
            StringBuilder maxPoints = new StringBuilder();

            for (Reservation reservation : unitReservations) {
                Date examDate = reservation.getFirstDate();
                SimpleDateFormat examDayFormat = new SimpleDateFormat("dd.MM.yyyy");
                if (examDay == null) {
                    examDay = examDayFormat.format(examDate);
                }

                if (earliestTime == null || examDate.before(earliestTime)) {
                    earliestTime = examDate;
                }

                if (room == null) {
                    for (Allocatable resource : reservation.getResources()) {
                        try {
                            resource.getClassification().getValueAsString(resource.getClassification().getAttribute("Raumname"), null);
                            room = resource.getName(null);
                            break; // Only the first room is needed
                        } catch (Exception e) {
                            // Ignore exceptions for resources without room names
                        }
                    }
                }

                totalDuration += Integer.parseInt(reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Dauer")).toString());
                names.append(reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Name"))).append("<br>");
                maxPoints.append(reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("max_punkte"))).append("/" + unitPoints + "<br>");
            }

            SimpleDateFormat examTimeFormat = new SimpleDateFormat("HH:mm");
            String examTimeStart = examTimeFormat.format(earliestTime);
            String examTimeEnd = examTimeFormat.format(new Date(earliestTime.getTime() + totalDuration * 60000));

            out.println("<tr>");
            if (examDay != null) { out.println("<td>" + examDay + "</td>"); } else {out.println("<td></td>");}
            if (examTimeStart != null) { out.println("<td>" + examTimeStart + " - " + examTimeEnd + "</td>"); } else {out.println("<td></td>");}
            if (room != null) { out.println("<td>" + room + "</td>"); } else {out.println("<td></td>");}
            if (unitName != null) { out.println("<td>" + unitName + "</td>"); } else {out.println("<td></td>");}
            if (totalDuration != 0) { out.println("<td>" + totalDuration + " Min.</td>"); } else {out.println("<td></td>");}
            if (names.length() != 0) { out.println("<td>" + names.toString() + "</td>"); } else {out.println("<td></td>");}
            if (maxPoints.length() != 0) { out.println("<td>" + maxPoints.toString() + "</td>"); } else {out.println("<td></td>");}
            out.println("</tr>");
        }

        out.println("</table>");
        out.println("</div>"); // table-container


        out.println( "</body>" );
        out.println( "</html>" );
        out.close();



        // for (Reservation reservation:reservations) {
        //     out.println("<p>");

        //     out.println(reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Name")));

        //     // out.println(reservation.getName(null));
        //     // out.println("<br>");
        //     // // out.println(reservation.getResources());
        //     // out.println("<br>");
        //     // out.println("Resourcen: <br>");
        //     // for (Allocatable resource:reservation.getResources()) {
        //     //     out.println(resource.getName(null));
        //     //     out.println("<br>");
        //     // }
        //     // out.println("Dozierende: <br>");
        //     // for (Allocatable resource:reservation.getPersons()) {
        //     //     out.println(resource.getName(null));
        //     //     out.println("<br>");
        //     // }
        //     // out.println("<br>");
        //     // out.println(reservation.getFirstDate());
        //     // out.println("<br>");
        //     // // out.println(reservation.getAnnotationKeys());
        //     // // out.println("<br>");
        //     // // for (String key:reservation.getAnnotationKeys()) {
        //     // //     out.println(key + " : " + reservation.getAnnotation(key, null));
        //     // //     out.println("<br>");
        //     // // }

        //     // out.println("Dynamic Type Test <br>");
        //     // Classification classification = reservation.getClassification();
        //     // out.println(classification.getName(null));
        //     // for (Attribute attribute:classification.getAttributes()) {
        //     //     out.println(attribute.getKey() + " : " + classification.getValueForAttribute(attribute));
        //     //     out.println("<br>");
        //     // }


        //     out.println("</p>");
        // }

    }




    @GET
    @Produces(MediaType.TEXT_HTML)
    public void generatePage( @Context HttpServletRequest request, @Context HttpServletResponse response ) throws Exception {
        java.io.PrintWriter out = response.getWriter();
        response.setContentType("text/html; charset=ISO-8859-1");
        String linkPrefix = request.getPathTranslated() != null ? "../": "";

        DynamicType kursTyp = facade.getDynamicType("Kurs");

        ClassificationFilter kursFilter = kursTyp.newClassificationFilter();
        kursFilter.addRule("Kursname"
                ,new Object[][] {
                        {"starts", "STG-WWI"}
                }
        );
        Allocatable[] kurse = facade.getAllocatablesWithFilter(kursFilter.toArray());


        out.println( "<body>" );
        for (Allocatable kurs:kurse) {
            out.println("<p>");
            out.println("<a href=\"pruefungen/kurs?id="+kurs.getId()+"\">");
            out.println(kurs.getName(null));
            out.println("</a></p>");
        }
        out.println( "<hr>" );
        out.println( "</body>" );
        out.println( "</html>" );
        out.close();
    }

    @POST
    @Produces(MediaType.TEXT_HTML)
    public void importSemesterplan( @Context HttpServletRequest request, @Context HttpServletResponse response ) throws Exception {
        String eventId = request.getParameter("eventId");

        ReferenceInfo<Reservation> reservationId =  new ReferenceInfo<>(eventId, Reservation.class);
        Reservation resolve = facade.resolve(reservationId);
        Reservation editableReservation = facade.edit(resolve);
    }


    }