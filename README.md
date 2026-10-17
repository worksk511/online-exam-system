# Online Exam System

A console-based exam platform written in Java. Teachers create timed multiple-choice exams; students log in, sit an exam against the clock, get it marked instantly and can review their results history.

![Java](https://img.shields.io/badge/Java-17%2B-22d3ee?style=flat-square&logo=openjdk&logoColor=white&labelColor=0b1220)
![Type](https://img.shields.io/badge/app-console-3dfc9e?style=flat-square&labelColor=0b1220)

## Features

- **Two roles, one system:** teachers and students each get their own menu after logging in
- **Accounts:** registration with duplicate-username checks; passwords are stored as SHA-256 hashes, never in plain text
- **Exam builder:** teachers set a title, a time limit and any number of multiple-choice questions (2–6 options each)
- **Timed attempts:** the time remaining is shown on every question, and answers given after the deadline are not counted
- **Automatic marking:** score as a percentage, pass/fail against a 70% pass mark, and a question-by-question review showing the correct answers
- **Results history:** students see their own attempts; teachers see every student's results
- **Input validation:** menus and answers re-prompt on invalid input instead of crashing

## Run it

Requires Java 17 or later.

```bash
javac OnlineExamSystem.java
java OnlineExamSystem
```

Two demo accounts and two sample exams are loaded at start-up:

| Role | Username | Password |
|---|---|---|
| Teacher | `teacher1` | `teacher123` |
| Student | `student1` | `student123` |

## Example session

```text
=== Online Exam System ===
1) Log in   2) Register   0) Exit
> 1
Username: student1
Password: student123
Welcome, student1.

[Student] 1) Take an exam  2) My results  0) Log out
> 1
#1  General Knowledge       3 questions    5 min  (by teacher1)
#2  Java Basics             3 questions   10 min  (by teacher1)
Exam number: 2

Q1. Which keyword creates an object?   (9:59 left)
  1) class
  2) new
  3) this
  4) static
Your answer: 2
...
Submitted.
Score: 100% (3/3) - PASS (pass mark 70%)
```

## How it's structured

| Class | Responsibility |
|---|---|
| `User` | Username, hashed password and role (`TEACHER` / `STUDENT`) |
| `Question` | Question text, options and the index of the correct option (validated on creation) |
| `Exam` | Title, time limit, questions and author; immutable once created |
| `ExamResult` | Score, pass/fail, whether time ran out, and when it was taken |
| `OnlineExamSystem` | Registration, authentication, exam creation, marking and the console menus |

The marking logic (`submit`) is separate from the console input code, so it can be reused behind a web or GUI front end.

## Also in this repo

`Calculator.java`: a command-line calculator supporting arithmetic plus `sqrt`, `cbrt`, `pow`, `max`, `min`, `abs`, `ceil`, `floor`, `round`, `log`, `exp`, `sin`, `cos` and `tan`, with checks for division by zero and invalid input.

```bash
javac Calculator.java && java Calculator
```

## Background

Started in 2024 as a task for the InternCareer Java internship, then extended in 2026 with interactive menus, enforced time limits, password hashing, answer review and input validation.

---

Built by [Jahnvi Singh](https://jahnvi-singh.netlify.app) · Software Engineering @ ANU
