A high-concurrency food delivery backend system built with **Spring Boot 2.7, MyBatis-Plus, Redis, and MySQL**. The system is divided into two primary modules: Mobile Client (WeChat Mini-Program) and Management Console, supporting end-to-end order processing, real-time notifications, and data analytics.

Tech Stack
- Core Framework: Spring Boot, Spring Framework
- Persistence Layer: MyBatis,
- Database & Cache: MySQL 8.0, Redis (Spring Data Redis)
- Middleware & Tools: Spring Task, Apache POI, JWT, Swagger / Knife4j, AliCloud OSS
- Build Tool: Maven

Project Architecture
text
take-out
sky-common     Common utilities, constants, exception handlers, and context components
sky-pojo      Entities, Data Transfer Objects (DTOs), and View Objects (VOs)
sky-server    Core business logic (Controllers, Services, Mappers, Tasks, Aspects)
