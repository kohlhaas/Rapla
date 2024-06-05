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

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
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

    // @GET
    // @Path("kurs")
    // public void generateKurs( @Context HttpServletRequest request, @Context HttpServletResponse response ) throws Exception {
    //     java.io.PrintWriter out = response.getWriter();
    //     String kursId = request.getParameter("id");

    //     ReferenceInfo<Allocatable> kurs =  new ReferenceInfo<>(kursId, Allocatable.class);
    //     Allocatable resolve = facade.resolve(kurs);

    //     DynamicType pruefung = facade.getDynamicType("Pruefung");
    //     ClassificationFilter[] pruefungen = pruefung.newClassificationFilter().toArray();
    //     Promise<Collection<Reservation>> allePruefungen = facade.getReservationsForAllocatable(new Allocatable[] {resolve}, null, null, pruefungen);
    //     Collection<Reservation> reservations = SynchronizedCompletablePromise.waitFor(allePruefungen, 10000,logger);

    //     out.println( "<html>" );
    //     out.println( "<head>" );
    //     out.println("  <title>Kurs</title>");
    //     out.println("</head>" );

    //     out.println( "<body>" );
    //     for (Reservation reservation:reservations) {
    //         out.println("<p>");

    //         out.println(reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Beschreibung")));

    //         // out.println(reservation.getName(null));
    //         // out.println("<br>");
    //         // // out.println(reservation.getResources());
    //         // out.println("<br>");
    //         // out.println("Resourcen: <br>");
    //         // for (Allocatable resource:reservation.getResources()) {
    //         //     out.println(resource.getName(null));
    //         //     out.println("<br>");
    //         // }
    //         // out.println("Dozierende: <br>");
    //         // for (Allocatable resource:reservation.getPersons()) {
    //         //     out.println(resource.getName(null));
    //         //     out.println("<br>");
    //         // }
    //         // out.println("<br>");
    //         // out.println(reservation.getFirstDate());
    //         // out.println("<br>");
    //         // // out.println(reservation.getAnnotationKeys());
    //         // // out.println("<br>");
    //         // // for (String key:reservation.getAnnotationKeys()) {
    //         // //     out.println(key + " : " + reservation.getAnnotation(key, null));
    //         // //     out.println("<br>");
    //         // // }

    //         // out.println("Dynamic Type Test <br>");
    //         // Classification classification = reservation.getClassification();
    //         // out.println(classification.getName(null));
    //         // for (Attribute attribute:classification.getAttributes()) {
    //         //     out.println(attribute.getKey() + " : " + classification.getValueForAttribute(attribute));
    //         //     out.println("<br>");
    //         // }


    //         out.println("</p>");
    //     }
    //     // out.println( "<hr>" );
    //     out.println( "</body>" );
    //     out.println( "</html>" );
    //     out.close();
    // }



    @GET
    @Path("kurs")
    public void generateKurs(@Context HttpServletRequest request, @Context HttpServletResponse response) throws Exception {
        java.io.PrintWriter out = response.getWriter();
        String kursId = request.getParameter("id");

        ReferenceInfo<Allocatable> kurs = new ReferenceInfo<>(kursId, Allocatable.class);
        Allocatable resolve = facade.resolve(kurs);

        DynamicType pruefung = facade.getDynamicType("Pruefung");
        ClassificationFilter[] pruefungen = pruefung.newClassificationFilter().toArray();
        Promise<Collection<Reservation>> allePruefungen = facade.getReservationsForAllocatable(new Allocatable[]{resolve}, null, null, pruefungen);
        Collection<Reservation> reservations = SynchronizedCompletablePromise.waitFor(allePruefungen, 10000, logger);

        String template = readHtmlTemplate("template.html");
        String content = generateContent(reservations);
        String finalHtml = template.replace("{content}", content);

        response.setContentType("text/html; charset=ISO-8859-1");
        out.write(finalHtml);
        out.close();
    }

    private String readHtmlTemplate(String filePath) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String currentLine;
            while ((currentLine = br.readLine()) != null) {
                contentBuilder.append(currentLine).append("\n");
            }
        }
        return contentBuilder.toString();
    }

    private String generateContent(Collection<Reservation> reservations) {
        StringBuilder contentBuilder = new StringBuilder();
        for (Reservation reservation : reservations) {
            contentBuilder.append("<p>")
                          .append(reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Beschreibung")))
                          .append("</p>\n");
        }
        return contentBuilder.toString();
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