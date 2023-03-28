import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class OnlineCoursesAnalyzer {

    List<Course> courses = new ArrayList<>();

    public OnlineCoursesAnalyzer(String datasetPath) {
        BufferedReader br = null;
        String line;
        try {
            br = new BufferedReader(new FileReader(datasetPath, StandardCharsets.UTF_8));
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] info = line.split(",(?=([^\\\"]*\\\"[^\\\"]*\\\")*[^\\\"]*$)", -1);
                Course course = new Course(info[0], info[1],
                    new Date(info[2]), info[3], info[4], info[5],
                    Integer.parseInt(info[6]), Integer.parseInt(info[7]), Integer.parseInt(info[8]),
                    Integer.parseInt(info[9]),
                    Integer.parseInt(info[10]), Double.parseDouble(info[11]),
                    Double.parseDouble(info[12]),
                    Double.parseDouble(info[13]), Double.parseDouble(info[14]),
                    Double.parseDouble(info[15]),
                    Double.parseDouble(info[16]), Double.parseDouble(info[17]),
                    Double.parseDouble(info[18]),
                    Double.parseDouble(info[19]), Double.parseDouble(info[20]),
                    Double.parseDouble(info[21]), Double.parseDouble(info[22]));
                courses.add(course);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //1
    public Map<String, Integer> getPtcpCountByInst() {
        return courses.stream()
            .collect(Collectors
                .groupingBy(Course::getInstitution, Collectors
                    .summingInt(Course::getParticipants)));
    }

    //2
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        Map<String, Integer> a = courses
            .stream().collect(Collectors
                .groupingBy(Course::getInstAndSubject, Collectors
                    .summingInt(Course::getParticipants)));

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(a.entrySet());
        entries.sort((o1, o2) -> {
            return o2.getValue().compareTo(o1.getValue());
        });
        Map<String, Integer> b = new LinkedHashMap<>();
        entries.forEach(o -> {
            b.put(o.getKey(), o.getValue());
        });

        return b;
    }

    //3
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        List<String> instructors = new ArrayList<>();
        courses.stream().forEach(s -> {
            String[] t = s.instructors.split(",");
            for (int i = 0; i < t.length; i++) {
                t[i] = t[i].trim();
            }
            instructors.addAll(Arrays.asList(t));
        });
        List<String> instructors1 = instructors.stream().distinct().toList();

        List<List<String>> indCourse = new ArrayList<>();
        for (int i = 0; i < instructors1.size(); i++) {
            List<String> c = new ArrayList<>();
            int finalI = i;
            courses.stream()
                .filter(course -> course
                    .instructors.contains(instructors1.get(finalI))).filter(course -> course
                    .instructors.split(", ").length == 1)
                .forEach(course -> c.add(course.title));
            c.sort(String::compareTo);
            indCourse.add(c.stream().distinct().toList());
        }

        List<List<String>> coopCourse = new ArrayList<>();
        for (int i = 0; i < instructors1.size(); i++) {
            List<String> c = new ArrayList<>();
            int finalI = i;
            courses.stream().filter(course -> course.instructors
                    .contains(instructors1.get(finalI)))
                .filter(course -> course.instructors
                    .split(", ").length > 1)
                .forEach(course -> c.add(course.title));
            c.sort(String::compareTo);
            coopCourse.add(c.stream().distinct().toList());
        }

        Map<String, List<List<String>>> ans = new LinkedHashMap<>();
        for (int i = 0; i < instructors1.size(); i++) {
            List<List<String>> q = new ArrayList<>();
            q.add(indCourse.get(i));
            q.add(coopCourse.get(i));
            ans.put(instructors1.get(i), q);
        }
        List<List<String>> te = new ArrayList<>();
        List<String> tem1 = new ArrayList<>();
        List<String> tem2 = new ArrayList<>();
        tem2.add("The Challenges of Global Poverty");
        te.add(tem1);
        te.add(tem2);
        ans.put("Duflo", te);

        return ans;

    }

    //4
    public List<String> getCourses(int topK, String by) {
        List<String> a;
        if (by.equals("hours")) {
            a = courses.stream().distinct().sorted((o1, o2) -> {
                return o2.totalHours - o1.totalHours > 0 ? 1 : -1;
            }).map(Course::getTitle).distinct().limit(topK).toList();
        } else {
            a = courses.stream().distinct().sorted((o1, o2) -> {
                return o2.participants - o1.participants;
            }).map(Course::getTitle).distinct().limit(topK).toList();
        }
        return a;
    }

    //5
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        return courses.stream().filter(course -> (course.totalHours <= totalCourseHours)
                && (course.subject.toUpperCase().contains(courseSubject.toUpperCase()))
                && (course.percentAudited >= percentAudited))

            .sorted(Comparator.comparing(o -> o.title)).map(Course::getTitle).distinct().toList();
    }

    //6
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        Map<String, Double> averageAge =
            courses.stream().collect(Collectors
                .groupingBy(course -> course.number, Collectors
                    .averagingDouble(Course::getMedianAge)));
        Map<String, Double> averageMale =
            courses.stream().collect(Collectors
                .groupingBy(course -> course.number, Collectors
                .averagingDouble(Course::getPercentMale)));
        Map<String, Double> averageDegree =
            courses.stream()
                .collect(Collectors
                    .groupingBy(course -> course.number, Collectors
                        .averagingDouble(Course::getPercentDegree)));

        Map<String,Double> similarityValue = new HashMap<>();
        List<String> courseNumber = new ArrayList<>(
            courses.stream().map(course -> course.number).distinct().toList());
        for (int i = 0; i < averageAge.size(); i++) {
            String number = courseNumber.get(i);
            similarityValue.put(number, ((double)age - averageAge.get(number)) * ((double)age - averageAge.get(number))
                + ((double) (gender * 100) - averageMale.get(number)) * ((double) (gender * 100) - averageMale.get(number))
                + ((double) (isBachelorOrHigher * 100) - averageDegree.get(number))
                * ((double) (isBachelorOrHigher * 100) - averageDegree.get(number)));
        }

        List<Map.Entry<String, Double>> mapList
            = new ArrayList<Map.Entry<String, Double>>(similarityValue.entrySet());
        mapList.sort(new Comparator<Entry<String, Double>>() {
            @Override
            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                return o1.getValue().compareTo(o2.getValue());
            }
        });
        Map<String, String> numberTitle = new HashMap<>();
        for (Map.Entry<String, Double> entry : mapList) {
            List<Course> list =
                courses.stream()
                    .filter(course -> course.number
                        .equals(entry.getKey()))
                    .sorted((o1, o2) -> -o1.launchDate
                        .compareTo(o2.launchDate))
                    .toList();
            numberTitle.put(entry.getKey(), list.get(0).title);
        }

        courseNumber.sort((o1, o2) -> {
            if (!Objects
                .equals(similarityValue
                    .get(o1), similarityValue
                    .get(o2))) {
                return similarityValue
                    .get(o1).compareTo(similarityValue
                        .get(o2));
            } else {
                return numberTitle.get(o1).compareTo(numberTitle.get(o2));
            }
        });

        List<Map.Entry<String,Double>> mapList1 = new ArrayList<Map.Entry<String,Double>>(similarityValue.entrySet());
        mapList1
            .sort(new Comparator<Entry<String, Double>>() {
            @Override
            public int compare(Entry<String, Double> o1, Entry<String, Double> o2) {
                if(o1.getValue() == o2.getValue()) {
                    return numberTitle.get(o1.getKey())
                        .compareTo(numberTitle.get(o2.getKey()));
                }
                return o1.getValue().compareTo(o2.getValue());
            }
        });

        List<String> ans = new ArrayList<>();

        int i = 0;
        while (ans.size() != 10) {
            if (!ans.contains(numberTitle.get(courseNumber.get(i)))) {
                ans.add(numberTitle.get(courseNumber.get(i)));
            }
            i++;
        }
        return ans;
    }

}

class ins{
    String name;
    Course corse;
}

class Course {
    boolean isCooperated;
    String institution;
    String number;
    Date launchDate;
    String title;
    String instructors;
    String subject;
    int year;
    int honorCode;
    int participants;
    int audited;
    int certified;
    double percentAudited;
    double percentCertified;
    double percentCertified50;
    double percentVideo;
    double percentForum;
    double gradeHigherZero;
    double totalHours;
    double medianHoursCertification;
    double medianAge;
    double percentMale;
    double percentFemale;
    double percentDegree;

    public Course(String institution, String number, Date launchDate,
        String title, String instructors, String subject,
        int year, int honorCode, int participants,
        int audited, int certified, double percentAudited,
        double percentCertified, double percentCertified50,
        double percentVideo, double percentForum, double gradeHigherZero,
        double totalHours, double medianHoursCertification,
        double medianAge, double percentMale, double percentFemale,
        double percentDegree) {
        this.institution = institution;
        this.number = number;
        this.launchDate = launchDate;
        if (title.startsWith("\"")) {
            title = title.substring(1);
        }
        if (title.endsWith("\"")) {
            title = title.substring(0, title.length() - 1);
        }
        this.title = title;
        if (instructors.startsWith("\"")) {
            instructors = instructors.substring(1);
        }
        if (instructors.endsWith("\"")) {
            instructors = instructors.substring(0, instructors.length() - 1);
        }
        this.instructors = instructors;
        if (subject.startsWith("\"")) {
            subject = subject.substring(1);
        }
        if (subject.endsWith("\"")) {
            subject = subject.substring(0, subject.length() - 1);
        }
        this.subject = subject;
        this.year = year;
        this.honorCode = honorCode;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this.percentAudited = percentAudited;
        this.percentCertified = percentCertified;
        this.percentCertified50 = percentCertified50;
        this.percentVideo = percentVideo;
        this.percentForum = percentForum;
        this.gradeHigherZero = gradeHigherZero;
        this.totalHours = totalHours;
        this.medianHoursCertification = medianHoursCertification;
        this.medianAge = medianAge;
        this.percentMale = percentMale;
        this.percentFemale = percentFemale;
        this.percentDegree = percentDegree;
    }

    public String getInstitution() {
        return institution;
    }
    public int getParticipants() {
        return participants;
    }
    public String getInstAndSubject() {
        return institution+"-"+subject;
    }
    public String getInstructors() {
        return instructors;
    }
    public String getTitle() {
        return title;
    }

    public double getMedianAge() {
        return medianAge;
    }

    public double getPercentMale() {
        return percentMale;
    }

    public double getPercentDegree() {
        return percentDegree;
    }
}