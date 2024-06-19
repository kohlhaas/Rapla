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
import org.rapla.plugin.abstractcalendar.server.AbstractHTMLCalendarPage;
import org.rapla.scheduler.Promise;
import org.rapla.scheduler.sync.SynchronizedCompletablePromise;
import org.rapla.server.internal.RaplaStatusEntry;
import org.rapla.server.internal.ServerContainerContext;
import org.rapla.entities.domain.Appointment;

// import microsoft.exchange.webservices.data.core.service.item.Appointment;

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

    public String formatDate(Date date, String formatPattern){
        SimpleDateFormat formatter = new SimpleDateFormat(formatPattern);
        return formatter.format(date);
    }

    public int getSemesterForDate(Date date, String[][] semesterDates) {
        String dateString = formatDate(date, "yyyy-MM-dd");
        int semester = 0;
        for (int i = 0; i < 6; i++) {
            if (dateString.compareTo(semesterDates[i][0]) >= 0 && dateString.compareTo(semesterDates[i][1]) <= 0) {
                semester = i + 1;
                break;
            } 
        }
        if (dateString.compareTo(semesterDates[5][1]) > 0) {
            semester = 6;
        }
        if (dateString.compareTo(semesterDates[0][0]) < 0) {
            semester = 1;
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

    public Reservation getLectureOfExam(Reservation exam, Collection<Reservation> lectures) {
        for (Reservation lecture:lectures) {
            if (exam.getClassification().getValueForAttribute(exam.getClassification().getAttribute("Name")).equals(lecture.getClassification().getValueForAttribute(lecture.getClassification().getAttribute("Name")))){
                return lecture;
            }
        }
        return null;
    }

    @GET
    @Path("kurs")
    public void generateKurs( @Context HttpServletRequest request, @Context HttpServletResponse response ) throws Exception {
        java.io.PrintWriter out = response.getWriter();
        String kursId = request.getParameter("id");
        
        ReferenceInfo<Allocatable> kurs =  new ReferenceInfo<>(kursId, Allocatable.class);
        Allocatable resolve = facade.resolve(kurs);
        
        String courseName = resolve.getName(null);
        
        Date currentDate = new Date();
        int currentSemester = getSemesterForDate(currentDate, createSemesterDates(courseName));

        DynamicType exam = facade.getDynamicType("Pruefung");
        ClassificationFilter[] exams = exam.newClassificationFilter().toArray();
        Promise<Collection<Reservation>> allExams = facade.getReservationsForAllocatable(new Allocatable[] {resolve}, null, null, exams);
        Collection<Reservation> examsReservations = SynchronizedCompletablePromise.waitFor(allExams, 10000,logger);
        
        DynamicType lecture = facade.getDynamicType("Lehrveranstaltung");
        ClassificationFilter[] lectures = lecture.newClassificationFilter().toArray();
        Promise<Collection<Reservation>> allLectures = facade.getReservationsForAllocatable(new Allocatable[] {resolve}, null, null, lectures);
        Collection<Reservation> lecturesReservations = SynchronizedCompletablePromise.waitFor(allLectures, 10000,logger);


        out.println( "<html>" );
        out.println( "<head>" );
        out.println("<title>Prüfungsverzeichnis: " + courseName + "</title>"); 
        out.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">");
        out.println("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/jspdf/2.5.1/jspdf.umd.min.js\"></script>");
        out.println("<script src=\"https://cdnjs.cloudflare.com/ajax/libs/html2canvas/1.4.1/html2canvas.min.js\"></script>");
        out.println(AbstractHTMLCalendarPage.getCssLine(request, "pruefungsansicht.css"));
        out.println("</head>" );

        out.println( "<body>" );
        out.println("<div class=\"save-button-container\">");
        out.println("<button onclick=\"saveAsPDF()\" class=\"save-button\">Save as PDF</button>");
        out.println("</div>");
        out.println("<div class=\"container\">");
        out.println("<header> <h1>Prüfungsverzeichnis - Kurs " + courseName + "</h1></header>");

        out.println("<div class=\"filter-semester-container\">");
        out.println("<h2>Semester: ");
            out.println("<select name=\"semester\" id=\"dropdown_semester\">");
            for (int i=1; i<=6; i++) {
                if (i == currentSemester){
                    out.println("<option value=\"" + i + "\" selected>" + i + "</option>");
                } else {
                    out.println("<option value=\"" + i + "\">" + i + "</option>");
                }
            }
            out.println("</select>");
        out.println("</h2>");
        out.println("</div>");      // filter-semester-container
                
        // - - -  View lectures:
        out.println("<div class=\"lectures-container\">");
        out.println("<div class=\"vorlesungen-header\"> <h2>Vorlesungen</h2> </div>");      
        out.println("<div class=\"grid\" id=\"lectures-grid\">");     
        // Generated in JS   
        out.println("</div>");      // grid
        out.println("</div>");      // lectures-container
        

        // - - -  View exams:
        out.println("<div class=\"vorlesungen-header\"> <h2>Prüfungen</h2> </div>");
        out.println("<div class=\"table-container\">");
        out.println("<div id=\"exams-table-container\">");
        // Generated in JS
        out.println("</div>"); // exams-table-container
        out.println("</div>"); // table-container


        out.println("<script>");       
        // Store exam performances in JS
        out.println("const exam_performances = [");
        for (Reservation reservation:examsReservations) {
            String lecturers_list = "";
            for (Allocatable resource:reservation.getPersons()) {
                lecturers_list += "" + resource.getName(null) + "; ";
            }
            if (lecturers_list.length() > 0){
                lecturers_list = lecturers_list.substring(0, lecturers_list.length() - 2);
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
            int semesterExam = getSemesterForDate(reservation.getFirstDate(), createSemesterDates(courseName));
            Reservation lectureOfExam = getLectureOfExam(reservation, lecturesReservations);
            int semesterLectures = semesterExam;
            try{
                semesterLectures = getSemesterForDate(lectureOfExam.getFirstDate(), createSemesterDates(courseName));
            }
            catch (Exception e) {
                // Ignore exceptions 
            }

            Appointment[] appointments = reservation.getAppointments();
            String submissionDates = "";
            String presentationDates = "";
            String examDates = "";

            for (Appointment appointment:appointments) {
                if (appointment.getComment() != null) {
                    switch (appointment.getComment()) {
                        case "Abgabe":
                            submissionDates += "\"" + formatDate(appointment.getStart(), "dd.MM.yyyy") + "\", ";
                            break;
                        case "Präsentation":
                            presentationDates += "\"" + formatDate(appointment.getStart(), "dd.MM.yyyy") + "\", ";
                            break;
                        case "Klausur":
                            examDates += appointment.getStart();
                            break;
                        default:
                            break;
                    }
                }
            }
            
            String datesList = "";
            if (submissionDates.length() > 0) {
                submissionDates = submissionDates.substring(0, submissionDates.length() - 2);
                datesList += "abgabe: [" + submissionDates + "], ";
            }
            if (presentationDates.length() > 0) {
                presentationDates = presentationDates.substring(0, presentationDates.length() - 2);
                datesList += "praesentation: [" + presentationDates + "], ";
            }
            if (examDates.length() > 0) {
                datesList += "klausur: \"" + examDates + "\", ";
            }
            if (datesList.length() > 0){
                datesList = datesList.substring(0, datesList.length() - 2);
            }

            out.println("{");
            out.println("lecture_name: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Name")) + "\",");
            out.println("unit_name: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("unit_name")) + "\",");
            out.println("type: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Pruefungsart")) + "\",");
            out.println("description: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Beschreibung")) + "\",");
            out.println("maximal_points: " + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("max_punkte")) + ",");
            out.println("lecturer: \"" + lecturers_list + "\",");
            out.println("duration: " + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("Dauer")) + ",");
            out.println("room: \"" + exam_room_name + "\",");
            out.println("link: \"" + reservation.getClassification().getValueForAttribute(reservation.getClassification().getAttribute("link")) + "\",");
            out.println("semester_exams: " + semesterExam + ",");
            out.println("semester_lectures: " + semesterLectures + ",");
            out.println("dates: {" + datesList + "}");
            out.println("},");
        }
        out.println("];");
        out.println("console.log(exam_performances);");

        out.println("function convertDateStringToDDMMYYYY(dateString) {\r\n" + //
                        "            const date = new Date(dateString);\r\n" + //
                        "            const day = String(date.getUTCDate()).padStart(2, '0');\r\n" + //
                        "            const month = String(date.getUTCMonth() + 1).padStart(2, '0');\r\n" + //
                        "            const year = date.getUTCFullYear();\r\n" + //
                        "            return `${day}.${month}.${year}`;\r\n" + //
                        "        }");

        out.println("function convertDateStringToHHMM(dateString) {\r\n" + //
                        "            const date = new Date(dateString);\r\n" + //
                        "            const hours = String(date.getUTCHours()).padStart(2, '0');\r\n" + //
                        "            const minutes = String(date.getUTCMinutes()).padStart(2, '0');\r\n" + //
                        "            return `${hours}:${minutes}`;\r\n" + //
                        "        }");

        out.println("function calculateEndTime(startTime, durationMinutes) {\r\n" + //
                        "            const [startHours, startMinutes] = startTime.split(':').map(Number);\r\n" + //
                        "            const totalMinutes = startHours * 60 + startMinutes + durationMinutes;\r\n" + //
                        "            const endHours = Math.floor(totalMinutes / 60) % 24; \r\n" + //
                        "            const endMinutes = totalMinutes % 60;\r\n" + //
                        "            const formattedEndHours = String(endHours).padStart(2, '0');\r\n" + //
                        "            const formattedEndMinutes = String(endMinutes).padStart(2, '0');\r\n" + //
                        "            return `${formattedEndHours}:${formattedEndMinutes}`;\r\n" + //
                        "        }");

        out.println("function filterExamPerformancesByLecturesSemester(exam_performances, semester) {\r\n" + //
                        "return exam_performances.filter(exam_performances => exam_performances.semester_lectures === semester);}"
        );

        out.println("function filterExamPerformancesByExamsSemester(exam_performances, semester) {\r\n" + //
                        "return exam_performances.filter(exam_performances => exam_performances.semester_exams === semester);}"
        );

        out.println("function filterExamPerformancesByType(exam_performances, type) {\r\n" + 
                        "return exam_performances.filter(exam_performance => exam_performance.type.toLowerCase() === type.toLowerCase());}"
        );
        
        out.println("function aggregateExamsPerUnit(exams) {\r\n" + //
                        "const aggregatedData = {};\r\n" + //
                        "exams.forEach(exam => {\r\n" + //
                            "const unitName = exam.unit_name;\r\n" + //
                            "if (!aggregatedData[unitName]) {\r\n" + //
                                "aggregatedData[unitName] = {\r\n" + //
                                    "day: convertDateStringToDDMMYYYY(exam.dates.klausur),\r\n" + //
                                    "start_time: convertDateStringToHHMM(exam.dates.klausur),\r\n" + //
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
        
                        "tableContent += `<h4><div class=\"card-heading\">${exam_performance.lecture_name} - ${exam_performance.unit_name}</div></h4>`; \r\n" + //
                        "tableContent += `<table>`; \r\n" + //
                        "tableContent += `<tr><th>Prüfungsart</th><td>${exam_performance.type}</td></tr>`; \r\n" + //
                        "tableContent += `<tr><th>Prüfungsdetails</th><td>${exam_performance.description}</td></tr>`; \r\n" + //
                        "tableContent += `<tr><th>Termine</th><td class=\"termine-zelle\"><table class=\"termine-tabelle\">`; \r\n" + //
                        "if (exam_performance.dates.praesentation != null) { \r\n" + //+
                            "tableContent += `<tr class=\"termine-tabelle\"><td class=\"termine-tabelle\">Präsentation:</td><td class=\"termine-tabelle\">${exam_performance.dates.praesentation[0]}`; \r\n" + //
                            "for (let i = 1; i < exam_performance.dates.praesentation.length; i++) { \r\n" + //
                                "tableContent += `<br>${exam_performance.dates.praesentation[i]}`; \r\n" + //
                            "} \r\n" + //
                            "tableContent += `</td></tr>`; \r\n" + //
                        "} \r\n" + //
                        "if (exam_performance.dates.abgabe != null) { \r\n" + //
                        "tableContent += `<tr class=\"termine-tabelle\"><td class=\"termine-tabelle\">Abgabe:</td><td class=\"termine-tabelle\">${exam_performance.dates.abgabe[0]}`; \r\n" + //
                            "for (let i = 1; i < exam_performance.dates.abgabe.length; i++) { \r\n" + //
                                "tableContent += `<br>${exam_performance.dates.abgabe[i]}`; \r\n" + //
                            "} \r\n" + //
                            "tableContent += `</td></tr>`; \r\n" + //
                        "} \r\n" + //
                        "if (exam_performance.dates.klausur != null) { \r\n" + //
                            "tableContent += `<tr class=\"termine-tabelle\"><td class=\"termine-tabelle\">Klausur:</td><td class=\"termine-tabelle\">${convertDateStringToDDMMYYYY(exam_performance.dates.klausur)}</td></tr>`; \r\n" + //
                        "} \r\n" + //
                        "tableContent += `</table></td></tr>`; \r\n" + //

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
                        "tableContent += `<td>${exam_performance.day}</td>`; \r\n" + //
                        "tableContent += `<td>${exam_performance.start_time} - ${calculateEndTime(exam_performance.start_time, exam_performance.duration)}</td>`; \r\n" + //
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
                        "const filteredExamPerformances = filterExamPerformancesByLecturesSemester(exam_performances, selectedSemesterValue); \r\n" + //
                        "renderLecturesView(filteredExamPerformances); \r\n" + //
                        "const filteredExams = filterExamPerformancesByType(filterExamPerformancesByExamsSemester(exam_performances, selectedSemesterValue), \"Klausur\"); \r\n" + //
                        "const aggregatedFilteredExams = aggregateExamsPerUnit(filteredExams); \r\n" + //
                        "renderExamsView(aggregatedFilteredExams); \r\n" + //
                    "});"
        );
        out.println("document.getElementById(\"dropdown_semester\").dispatchEvent(new Event(\"change\"));");

        // Save as PDF
        out.println("async function saveAsPDF() { \r\n" + //
                        "const { jsPDF } = window.jspdf; \r\n" + //
                        "const saveButton = document.querySelector('.save-button-container'); \r\n" + //
                        "saveButton.classList.add('hidden'); \r\n" + //
                        "const canvas = await html2canvas(document.body); \r\n" + //
                        "saveButton.classList.remove('hidden'); \r\n" + //
                        "const imgData = canvas.toDataURL('image/png'); \r\n" + //
                        "const pdf = new jsPDF('p', 'mm', 'a4'); \r\n" + //
                        "const imgWidth = 210; // A4 width in mm \r\n" + //
                        "const pageHeight = 295; // A4 height in mm \r\n" + //
                        "const imgHeight = canvas.height * imgWidth / canvas.width; \r\n" + //
                        "let heightLeft = imgHeight; \r\n" + //

                        "let position = 0; \r\n" + //
                        "pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight); \r\n" + //
                        "heightLeft -= pageHeight; \r\n" + //
                        "while (heightLeft >= 0) { \r\n" + //
                            "position = heightLeft - imgHeight; \r\n" + //
                            "pdf.addPage(); \r\n" + //
                            "pdf.addImage(imgData, 'PNG', 0, position, imgWidth, imgHeight); \r\n" + //
                            "heightLeft -= pageHeight; \r\n" + //
                        "} \r\n" + //

                        "pdf.save('webpage.pdf'); \r\n" + //
                    "};"
        );

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

        DynamicType kursTyp = facade.getDynamicType("Kurs");

        ClassificationFilter kursFilter = kursTyp.newClassificationFilter();
        kursFilter.addRule("Kursname"
                ,new Object[][] {
                        {"starts", "STG-WWI"}
                }
        );
        Allocatable[] kurse = facade.getAllocatablesWithFilter(kursFilter.toArray());

        Arrays.sort(kurse, new Comparator<Allocatable>() {
            @Override
            public int compare(Allocatable a1, Allocatable a2) {
                // Assuming you have a method to get the name from Allocatable
                String name1 = a1.getName(null);
                String name2 = a2.getName(null);
                return name1.compareTo(name2);
            }
        });

        out.println( "<html>" );
        out.println( "<head>" );
        out.println("<title>Kurswahl - Prüfungsübersicht</title>");
        out.println(AbstractHTMLCalendarPage.getCssLine(request, "pruefungsansicht.css"));
        out.println("</head>" );

        out.println( "<body>" );
        out.println( "<h1>Prüfungsübersicht - Kurswahl</h1>" );
        out.println( "<ul>" );
        for (Allocatable kurs:kurse) {
            out.println("<li><a href=\"pruefungen/kurs?id=" + kurs.getId() + "\" class=\"kurs-liste\">" + kurs.getName(null) + "</a></li>");
            // out.println("<li><a href=\"pruefungen/kurs?id=" + kurs.getId() + "\" class=\"kurs-liste\">" + kurs.getName(null) + "</a><button onclick=\"copyToClipboard('pruefungen/kurs?id=" + kurs.getId() + "')\">Copy Link</button></li>");

        }
        out.println( "</ul>" );
        out.println("<script>");
        
        // out.println("function copyToClipboard(text) {\r\n" + //
        //                 "            var textarea = document.createElement(\"textarea\");\r\n" + //
        //                 "            textarea.value = text;\r\n" + //
        //                 "            textarea.style.position = \"fixed\";\r\n" + //
        //                 "            textarea.style.opacity = 0;\r\n" + //
        //                 "            document.body.appendChild(textarea);\r\n" + //
        //                 "            textarea.select();\r\n" + //
        //                 "            document.execCommand(\"copy\");\r\n" + //
        //                 "            document.body.removeChild(textarea);\r\n" + //
        //                 "            alert(\"Link copied to clipboard: \" + text);\r\n" + //
        //                 "        }");

        out.println("</script>");
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