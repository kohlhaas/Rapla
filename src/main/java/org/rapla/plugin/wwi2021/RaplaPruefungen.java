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
import java.util.Collection;



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

        DynamicType pruefung = facade.getDynamicType("Pruefung");
        ClassificationFilter[] pruefungen = pruefung.newClassificationFilter().toArray();
        Promise<Collection<Reservation>> allePruefungen = facade.getReservationsForAllocatable(new Allocatable[] {resolve}, null, null, pruefungen);
        Collection<Reservation> reservations = SynchronizedCompletablePromise.waitFor(allePruefungen, 10000,logger);
        // String linkPrefix = request.getPathTranslated() != null ? "../" : "";


        out.println( "<html>" );
        out.println( "<head>" );
        out.println("<title>Kurs: " + kursId + "</title>"); 
        out.println("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        // out.println("  <link REL=\"stylesheet\" href=\"" + linkPrefix + "pruefungsansicht.css\" type=\"text/css\">");
        out.println("  <link REL=\"stylesheet\" href=\"pruefungsansicht.css\" type=\"text/css\">");
        out.println("</head>" );

        out.println( "<body>" );
        out.println("<div class=\"container\">");
        out.println("<header>\r\n" + //
                        "            <h1>Prüfungsverzeichnis - Kurs WWI2021F</h1>\r\n" + //
                        "        </header>\r\n" + //
                        "        <div class=\"filter\">\r\n" + //
                        "            <p>Semester: </p>\r\n" + //
                        "        </div>\r\n" + //
                        "        <div class=\"vorlesungen-header\">\r\n" + //
                        "            <h2>Vorlesungen</h2>\r\n" + //
                        "        </div>\r\n" + //
                        "        <div class=\"grid\" id=\"vorlesungen-grid\"></div>");
        out.println("</div>");

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
        

        out.println("<div class=\"table-container\">\r\n" + //
                        "            <h2>Klausuren / Prüfungsleistungen</h2>");

        
        out.println("</div>");


        out.println("<script>");
        out.println("const grid = document.getElementById(\"vorlesungen-grid\");");
        out.println("const card = document.createElement(\"div\");\r\n" + //
                            "            card.className = \"card\";\r\n" + //
                            "            let tableContent = '';");


        for (Reservation reservation:reservations) {
            out.println("tableContent += `<h4><b>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Name")) + " - " + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("unit_name")) + "</h4>`;");
            out.println("tableContent += `<table>`;");
            out.println("tableContent += `<tr><th>Prüfungsart</th><td>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Pruefungsart")) + "</td></tr>`;");
            out.println("tableContent += `<tr><th>Prüfungsdetails</th><td>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Beschreibung")) + "</td></tr>`;");
            
            out.println("tableContent += `<tr><th>Termine</th><td>" + reservation.getSortedAppointments()+ "</td></tr>`;");
            // for (Appointment appointment:reservation.getAppointments()) {
            //     out.println(appointment.getStart());
            // }

            out.println("tableContent += `<tr><th>Max Punkte</th><td>" +reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("max_punkte")) + "</td></tr>`;");            
            String dozierende = "";
            for (Allocatable resource:reservation.getPersons()) {
                dozierende += resource.getName(null) + "; ";
            }
            out.println("tableContent += `<tr><th>Dozierende</th><td>" + dozierende + "</td></tr>`;");
            out.println("tableContent += `<tr><th>Moodle</th><td>" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("link")) + "</td></tr>`;");
            out.println("tableContent += `</table>`;");
        }
        out.println("card.innerHTML = `${tableContent}`;");
        out.println("grid.appendChild(card);");



        out.println("</script>");



        out.println( "</body>" );
        out.println( "</html>" );
        out.close();
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