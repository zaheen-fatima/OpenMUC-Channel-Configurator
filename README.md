# OpenMUC Channel Configurator 

A lightweight **OSGi-based servlet** for dynamically creating and updating 
`channels.xml` configuration files in **OpenMUC**.  

This project provides:
- 📡 **Modbus TCP & Modbus RTU** device support  
- ⚙️ Dynamic channel creation via a **web form**  
- 🗂️ Automatic persistence of configuration into `framework/conf/channels.xml`  
- 🖥️ Simple **HTML UI** for user-friendly interaction  

---

## 🔧 Features
- Supports **Modbus TCP** (IP/Port) and **Modbus RTU** (Serial COM settings).
- Add multiple channels with configurable:
  - Unit ID
  - Primary Table (Holding Register, Input Register, Coil, etc.)
  - Address
  - Datatype
- Auto-generates valid XML structure under:

---

## 🛠️ Technology Stack
- **Java** (Servlet, XML DOM API, OSGi Declarative Services)
- **OpenMUC Framework**
- **Jetty / Felix HTTP Service**
- **HTML + JS** (for form and dynamic channel creation)

---
