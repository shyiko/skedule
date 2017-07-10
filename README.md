<h1 align="center">
skedule
</h1>

<p align="center">
<a href="https://travis-ci.org/shyiko/skedule"><img src="https://travis-ci.org/shyiko/skedule.svg?branch=master" alt="Build Status"></a>
<a href="http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.github.shyiko.skedule%22%20AND%20a%3A%22skedule%22"><img src="http://img.shields.io/badge/maven_central-0.2.0-blue.svg?style=flat" alt="Maven Central"></a>
<a href="https://ktlint.github.io/"><img src="https://img.shields.io/badge/code%20style-%E2%9D%A4-FF4081.svg" alt="ktlint"></a>
</p>

<p align="center">
A human-friendly alternative to cron.<br>Designed after GAE's <a href="https://cloud.google.com/appengine/docs/standard/java/config/cronref#schedule_format">schedule</a> for Kotlin and/or Java 8+. 
</p>

Features:
- **TZ support**;
- **fluent** / **immutable** / **java.time.***-based API;
- **zero dependencies**.

## Usage

```xml
<dependency>
  <groupId>com.github.shyiko.skedule</groupId>
  <artifactId>skedule</artifactId>
  <version>0.2.0</version>
  <!-- omit classifier below if you plan to use this library in koltin -->
  <classifier>kalvanized</classifier>
</dependency>
```

> (java)

```java
// creating schedule from a string
Schedule.parse("every monday 09:00");

// programmatic construction
Schedule.at(LocalTime.of(9, 0)).every(DayOfWeek.MONDAY).toString().equals("every monday 09:00");

ZonedDateTime now = ZonedDateTime.parse("2007-12-03T10:15:30+02:00[Europe/Kiev]");
ZonedDateTime nxt = ZonedDateTime.parse("2007-12-10T09:00:00+02:00[Europe/Kiev]");

// determining "next" time
Schedule.parse("every monday 09:00").next(now).equals(nxt);

// iterating over (infinite) schedule
Schedule.parse("every monday 09:00").iterate(now)/*: Iterator<ZonedDateTime> */.next().equals(nxt);
```

#### Format

Schedule format is described in [GAE cron.xml reference](https://cloud.google.com/appengine/docs/standard/java/config/cronref#schedule_format).  
Here are some examples (taken from the official GAE documentation):

```
every 12 hours
every 5 minutes from 10:00 to 14:00
every day 00:00
every monday 09:00
2nd,third mon,wed,thu of march 17:00
1st monday of sep,oct,nov 17:00
1 of jan,april,july,oct 00:00
```

#### (example) Scheduling using ScheduledThreadPoolExecutor

```java
import com.github.shyiko.skedule.Schedule;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
executor.setRemoveOnCancelPolicy(true);
ZonedDateTime now = ZonedDateTime.now();
executor.schedule(
    () -> {
        System.out.println("TASK 'every 5 minutes from 12:00 to 12:30' EXECUTED");
        // re-schedule if needed
    },
    Schedule.from(LocalTime.NOON, LocalTime.of(12, 30)).every(5, ChronoUnit.MINUTES)
        .next(now).toEpochSecond() - now.toEpochSecond(),
    TimeUnit.SECONDS
);
executor.schedule(
    () -> {
        System.out.println("TASK 'every day 12:00' EXECUTED");
        // re-schedule if needed
    },
    Schedule.at(LocalTime.NOON).everyDay()
        .next(now).toEpochSecond() - now.toEpochSecond(),
    TimeUnit.SECONDS
);
```

> NOTE #1: Be careful with [java.util.TimerTask::cancel()](http://hg.openjdk.java.net/jdk8/jdk8/jdk/file/687fd7c7986d/src/share/classes/java/util/TimerTask.java#l119) (if you choose to go with java.util.Timer).

> NOTE #2: If you are thinking of using java.util.concurrent.DelayQueue - keep in mind that remove() operation is O(n).

> NOTE #3: If you have a huge number of scheduled tasks >= 10th of thousands you might want to consider switching to 
[Hierarchical Wheel Timer](https://pdfs.semanticscholar.org/0a14/2c84aeccc16b22c758cb57063fe227e83277.pdf)(s).   

## Development

```sh
git clone https://github.com/shyiko/skedule && cd skedule
./mvnw # shows how to build, test, etc. project
```

## Legal

All code, unless specified otherwise, is licensed under the [MIT](https://opensource.org/licenses/MIT) license.  
Copyright (c) 2017 Stanley Shyiko.
