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


    public int getSemesterForDate(Date date, String[][] semesterDates) {
        SimpleDateFormat dateDayFormat = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = dateDayFormat.format(date);
        int semester = 0;
        for (int i = 0; i < 6; i++) {
            if (dateString.compareTo(semesterDates[i][0]) >= 0 && dateString.compareTo(semesterDates[i][1]) <= 0) {
                semester = i + 1;
                break;
            }
        }
        return semester;
    }

    public String[][] createSemesterDates(String courseName) {
        int courseYear = Integer.parseInt(courseName.replaceAll("\\D", ""));
        String[][] semesterDates = new String[6][2];
        semesterDates[0][0] = courseYear + "-10-01";
        semesterDates[0][1] = (courseYear + 1) + "-03-05";
        semesterDates[1][0] = (courseYear + 1) + "-03-06";
        semesterDates[1][1] = (courseYear + 1) + "-08-25";
        semesterDates[2][0] = (courseYear + 1) + "-08-26";
        semesterDates[2][1] = (courseYear + 2) + "-02-01";
        semesterDates[3][0] = (courseYear + 2) + "-02-02";
        semesterDates[3][1] = (courseYear + 2) + "-11-01";
        semesterDates[4][0] = (courseYear + 2) + "-11-02";
        semesterDates[4][1] = (courseYear + 3) + "-04-25";
        semesterDates[5][0] = (courseYear + 3) + "-04-26";
        semesterDates[5][1] = (courseYear + 3) + "-09-30";
        return semesterDates;
    }

    @GET
    @Path("kurs")
    public void generateKurs( @Context HttpServletRequest request, @Context HttpServletResponse response ) throws Exception {
        java.io.PrintWriter out = response.getWriter();
        String kursId = request.getParameter("id");

        ReferenceInfo<Allocatable> kurs =  new ReferenceInfo<>(kursId, Allocatable.class);
        Allocatable resolve = facade.resolve(kurs);

        String courseName = resolve.getName(null);

        DynamicType pruefung = facade.getDynamicType("Pruefung");
        ClassificationFilter[] pruefungen = pruefung.newClassificationFilter().toArray();
        Promise<Collection<Reservation>> allePruefungen = facade.getReservationsForAllocatable(new Allocatable[] {resolve}, null, null, pruefungen);
        Collection<Reservation> reservations = SynchronizedCompletablePromise.waitFor(allePruefungen, 10000,logger);
        // String linkPrefix = request.getPathTranslated() != null ? "../" : "";


        out.println( "<html>" );
        out.println( "<head>" );
        out.println("<title>Prüfungsverzeichnis: " + courseName + "</title>"); 
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
        out.println("<header> <h1>Prüfungsverzeichnis - Kurs " + courseName + "</h1></header>");

        out.println("<div class=\"filter-semester-container\">");
        out.println("<h2>Semester: ");
            out.println("<select name=\"semester\" id=\"dropdown_semester\">");
                    out.println("<option value=\"1\">1</option>");
                    out.println("<option value=\"2\">2</option>");
                    out.println("<option value=\"3\">3</option>");
                    out.println("<option value=\"4\">4</option>");
                    out.println("<option value=\"5\">5</option>");             
                    out.println("<option value=\"6\" selected>6</option>");     //Standardwert hier ändern (TODO)
            out.println("</select>");
        out.println("</h2>");
        out.println("</div>");      // filter-semester-container
                
        // - - -  View lectures:
        out.println("<div class=\"lectures-container\">");
        out.println("<div class=\"container-header\"> <h2>Vorlesungen</h2> </div>");      
        out.println("<div class=\"grid\" id=\"lectures-grid\">");     
        // Generated in JS   
        out.println("</div>");      // grid
        out.println("</div>");      // lectures-container
        

        // - - -  View exams:
        out.println("<div class=\"table-container\">");
        out.println("<div class=\"container-header\"> <h2>Prüfungen</h2> </div>");
        out.println("<div id=\"exams-table-container\">");
        // Generated in JS
        out.println("</div>"); // exams-table-container
        out.println("</div>"); // table-container


        out.println("<script>");       
        // Store exam performances in JS
        out.println("const exam_performances = [");
        for (Reservation reservation:reservations) {
            String lecturers_list = "";
            for (Allocatable resource:reservation.getPersons()) {
                lecturers_list += "\"" + resource.getName(null) + "\", ";
            }
            String exam_room_name = "";
            for (Allocatable resource:reservation.getResources()) {
                try {
                    resource.getClassification().getValueAsString(resource.getClassification().getAttribute("Raumname"), null);
                    exam_room_name = resource.getName(null);
                    break; 
                } catch (Exception e) {
                    // Ignore exceptions for resources without room names
                }
            }
            // TODO: "Richtiges" Datum nutzen (statt getFirstDate())
            int semester = getSemesterForDate(reservation.getFirstDate(), createSemesterDates(courseName));

            out.println("{");
            out.println("lecture_name: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Name")) + "\",");
            out.println("unit_name: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("unit_name")) + "\",");
            out.println("type: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Pruefungsart")) + "\",");
            out.println("description: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Beschreibung")) + "\",");
            out.println("maximal_points: " + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("max_punkte")) + ",");
            out.println("lecturer: [" + lecturers_list + "],");
            out.println("duration: " + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Dauer")) + ",");
            out.println("room: \"" + exam_room_name + "\",");
            out.println("link: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("link")) + "\",");
            out.println("semester: " + semester + ",");
            // TODO: Logik für Datum einlesen (Klausur, Abgabe, Präsentation)
            out.println("dates: \"" + reservation.getSortedAppointments() + "\"");
            out.println("},");
        }
        out.println("];");
        out.println("console.log(exam_performances);");

        out.println("function filterExamPerformancesBySemester(exam_performances, semester) {\r\n" + //
                        "return exam_performances.filter(exam_performances => exam_performances.semester === semester);}"
        );

        out.println("function filterExamPerformancesByType(exam_performances, type) {\r\n" + 
                        "return exam_performances.filter(exam_performance => exam_performance.type.toLowerCase() === type.toLowerCase());}"
        );
        
        // TODO: "date" in Tag und Uhrzeit trennen
        out.println("function aggregateExamsPerUnit(exams) {\r\n" + //
                        "const aggregatedData = {};\r\n" + //
                        "exams.forEach(exam => {\r\n" + //
                            "const unitName = exam.unit_name;\r\n" + //
                            "if (!aggregatedData[unitName]) {\r\n" + //
                                "aggregatedData[unitName] = {\r\n" + //
                                    "date: exam.dates,\r\n" + //
                                    "day: exam.dates,\r\n" + //
                                    "start_time: exam.dates,\r\n" + //
                                    "room: exam.room,\r\n" + //
                                    "unit_name: exam.unit_name,\r\n" + //
                                    "duration: 0,\r\n" + //
                                    "lecture_name: [],\r\n" + //
                                    "maximal_points: []\r\n" + //
                                "};\r\n" + //
                            "}\r\n" + //
                            "aggregatedData[unitName].duration += exam.duration;\r\n" + //
                            "aggregatedData[unitName].lecture_name.push(exam.lecture_name);\r\n" + //
                            "aggregatedData[unitName].maximal_points.push(exam.maximal_points);\r\n" + //
                        "});\r\n" + //
                        "console.log(aggregatedData);\r\n" + //
                        "return Object.values(aggregatedData);\r\n" + //
                    "};"
        );

        
        // Lecture view
        out.println("const grid = document.getElementById('lectures-grid');");
        out.println("function renderLecturesView(exam_performances) {");
        out.println("grid.innerHTML = '';                                   \r\n" + //
                    "exam_performances.forEach(exam_performance => {         \r\n" + //
                        "const card = document.createElement(\"div\");      \r\n" + //
                        "card.className = \"card\";                         \r\n" + //
                        "let tableContent = '';                            \r\n" + //
        
                        "tableContent += `<h4><b>${exam_performance.lecture_name} - ${exam_performance.unit_name}</b></h4>`; \r\n" + //
                        "tableContent += `<table>`; \r\n" + //
                        "tableContent += `<tr><th>Prüfungsart</th><td>${exam_performance.type}</td></tr>`; \r\n" + //
                        "tableContent += `<tr><th>Prüfungsdetails</th><td>${exam_performance.description}</td></tr>`; \r\n" + //
                        "tableContent += `<tr><th>Termine</th><td>${exam_performance.dates}</td></tr>`; \r\n" + //
                        "if (exam_performance.maximal_points != null) { \r\n" + //
                            "tableContent += `<tr><th>Max. Punkte</th><td>${exam_performance.maximal_points}</td></tr>`; \r\n" + //
                        "} else { \r\n" + //
                            "tableContent += `<tr><th>Max. Punkte</th><td></td></tr>`; \r\n" + //
                        "} \r\n" + //
                        "tableContent += `<tr><th>Dozierende</th><td>${exam_performance.lecturer}</td></tr>`; \r\n" + //
                        "if (exam_performance.link != \"null\") { \r\n" + //
                            "tableContent += `<tr><th>Moodle</th><td><p><a href=\"${exam_performance.link}\">Link</a></p></td></tr>`; \r\n" + //
                        "} else { \r\n" + //
                            "tableContent += `<tr><th>Moodle</th><td></td></tr>`; \r\n" + //
                        "} \r\n" + //
                        "tableContent += `</table>`; \r\n" + //
                        "card.innerHTML = tableContent; \r\n" + //
                        "grid.appendChild(card); \r\n" + //
                    "}); \r\n" + //
                "}"
        );

        // Exam view
        out.println("const examsTable = document.getElementById('exams-table-container');");
        out.println("function renderExamsView(exam_performances) {");
        out.println("examsTable.innerHTML = '';                                   \r\n" + //
                    "let tableContent = '<table>';                                      \r\n" + //
                    "tableContent += `<tr><th>Datum</th><th>Uhrzeit</th><th>Raum</th><th>Modul</th><th>Dauer</th><th>Vorlesung / Modul-(teil)klausur</th><th>Klausuranteil</th></tr>`; \r\n" + //
                    "exam_performances.forEach(exam_performance => {         \r\n" + //
                        "tableContent += `<tr>`; \r\n" + //
                        "tableContent += `<td>${exam_performance.date}</td>`; \r\n" + //
                        "tableContent += `<td>${exam_performance.date}</td>`; \r\n" + //
                        "tableContent += `<td>${exam_performance.room}</td>`; \r\n" + //
                        "tableContent += `<td>${exam_performance.unit_name}</td>`; \r\n" + //
                        "tableContent += `<td>${exam_performance.duration} Min.</td>`; \r\n" + //
                        "tableContent += `<td>${exam_performance.lecture_name[0]}`; \r\n" + //
                        "for (let i = 1; i < exam_performance.lecture_name.length; i++) { \r\n" + //
                            "tableContent += `<br>${exam_performance.lecture_name[i]}`; \r\n" + //
                        "}\r\n" + //
                        "tableContent += `</td>`;\r\n" + //
                        "tableContent += `<td>${exam_performance.maximal_points[0]}/120`; \r\n" + //
                        "for (let i = 1; i < exam_performance.maximal_points.length; i++) { \r\n" + //
                            "tableContent += `<br>${exam_performance.maximal_points[i]}/120`;\r\n" + //
                        "}\r\n" + //
                        "tableContent += `</td>`; \r\n" + //
                        "tableContent += `</tr>`; \r\n" + //
                        "}); \r\n" + //
                    "tableContent += '</table>'; \r\n" + //
                    "examsTable.innerHTML += tableContent; \r\n" + //
                "}"
        );


        out.println("document.getElementById(\"dropdown_semester\").addEventListener(\"change\", function () {     \r\n" + //
                        "const selectedSemesterValue = parseInt(this.value); \r\n" + //
                        "const filteredExamPerformances = filterExamPerformancesBySemester(exam_performances, selectedSemesterValue); \r\n" + //
                        "renderLecturesView(filteredExamPerformances); \r\n" + //
                        "const filteredExams = filterExamPerformancesByType(filteredExamPerformances, \"Klausur\"); \r\n" + //
                        "const aggregatedFilteredExams = aggregateExamsPerUnit(filteredExams); \r\n" + //
                        "renderExamsView(aggregatedFilteredExams); \r\n" + //
                    "});"
        );
        out.println("document.getElementById(\"dropdown_semester\").dispatchEvent(new Event(\"change\"));");

        out.println("</script>");

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