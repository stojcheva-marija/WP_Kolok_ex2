package mk.ukim.finki.wp.kol2022.g2.service.impl;

import mk.ukim.finki.wp.kol2022.g2.model.Course;
import mk.ukim.finki.wp.kol2022.g2.model.Student;
import mk.ukim.finki.wp.kol2022.g2.model.StudentType;
import mk.ukim.finki.wp.kol2022.g2.model.exceptions.InvalidStudentIdException;
import mk.ukim.finki.wp.kol2022.g2.repository.CourseRepository;
import mk.ukim.finki.wp.kol2022.g2.repository.StudentRepository;
import mk.ukim.finki.wp.kol2022.g2.service.StudentService;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StudentServiceImpl implements StudentService, UserDetailsService {

    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public StudentServiceImpl(StudentRepository studentRepository, CourseRepository courseRepository, PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<Student> listAll() {
        return studentRepository.findAll();
    }

    @Override
    public Student findById(Long id) {
        return studentRepository.findById(id).orElseThrow(InvalidStudentIdException::new);
    }

    @Override
    public Student create(String name, String email, String password, StudentType type, List<Long> courseId, LocalDate enrollmentDate) {
        List<Course> courses = courseRepository.findAllById(courseId);
        return studentRepository.save(new Student(name, email, passwordEncoder.encode(password), type, courses, enrollmentDate));
    }

    @Override
    public Student update(Long id, String name, String email, String password, StudentType type, List<Long> coursesId, LocalDate enrollmentDate) {
        Student student = findById(id);
        student.setName(name);
        student.setEmail(email);
        student.setPassword(passwordEncoder.encode(password));
        student.setType(type);
        student.setCourses(courseRepository.findAllById(coursesId));
        student.setEnrollmentDate(enrollmentDate);
        return studentRepository.save(student);
    }

    @Override
    public Student delete(Long id) {
        Student student = findById(id);
        studentRepository.deleteById(id);
        return student;
    }

    @Override
    public List<Student> filter(Long courseId, Integer yearsOfStudying) {
        Course course = null;
       if(courseId != null){
           course = courseRepository.findById(courseId).orElse(null);
       }
       LocalDate dateToCompare = null;
       if(yearsOfStudying != null){
           dateToCompare = LocalDate.now().minusYears(yearsOfStudying);
       }

       if(course != null && dateToCompare != null){
           return studentRepository.findAllByCoursesContainingAndEnrollmentDateBefore(course, dateToCompare);
       }
       else if(course != null){
           return studentRepository.findAllByCoursesContaining(course);
       }
       else if(dateToCompare != null){
           return studentRepository.findAllByEnrollmentDateBefore(dateToCompare);
       }
       else{
           return studentRepository.findAll();
       }
    }

    @Override
    public UserDetails loadUserByUsername(String email ) throws UsernameNotFoundException {
        Student student = this.studentRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException(email));
        UserDetails userDetails = new org.springframework.security.core.userdetails.User(student.getEmail(),
                student.getPassword(), Stream.of(new SimpleGrantedAuthority("ROLE_" + student.getType())).collect(Collectors.toList()));
        return userDetails;
    }
}
