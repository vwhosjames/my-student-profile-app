package com.example.studentprofileapp;

/**
 * ProfileGenerator — a plain Java utility class.
 *
 * The profile-formatting logic lives here (not in Kotlin) so the app
 * demonstrates Java-Kotlin interoperability: MainActivity.kt creates an
 * instance of this class and calls generateProfile() on it.
 */
public class ProfileGenerator {

    /**
     * Builds a formatted student profile from the given information.
     *
     * @param studentName  full name of the student
     * @param studentId    student ID number
     * @param course       enrolled course
     * @param yearLevel    current year level
     * @param hometown     hometown of the student
     * @return a multi-line, formatted student profile String
     */
    public String generateProfile(String studentName, String studentId,
                                  String course, String yearLevel, String hometown) {
        return "STUDENT PROFILE\n"
                + "Name: " + studentName + "\n"
                + "Student ID: " + studentId + "\n"
                + "Course: " + course + "\n"
                + "Year: " + yearLevel + "\n"
                + "Hometown: " + hometown + "\n"
                + "Welcome, " + studentName + "!";
    }
}
