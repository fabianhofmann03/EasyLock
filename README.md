# EasyLock
EasyLock ist ein Schulprojekt von mir und 2 weiteren Kollegen.
Es nutzt einen magnetischen Schieberiegler der die Tür auf und zu sperrt und einen Hallsensor um den aktuellen Zustand der Tür zu erkennen.
Über eine App auf dem Handy kann das Schloss dann geöffnet, das Passwort geändert oder die Werte des Hallsensors konfiguriert werden.

# Schaltplan & Platine

Nun zu der Erklärung des Schaltplans. Falls du das Projekt selber nachbauen möchtest, findest du die [CAM-Daten](https://github.com/fabianhofmann03/EasyLock/blob/main/Schaltung%20%26%20Platine/CAM-Data.zip) hier.

<img src="https://github.com/fabianhofmann03/EasyLock/blob/main/Schaltung%20&%20Platine/Platine.jpg?raw=true" width="500" height="500">

## Microcontroller

Als Microcontroller haben wir den PIC16F1827 verwendet. 
<img src="https://raw.githubusercontent.com/fabianhofmann03/EasyLock/main/Schaltung%20%26%20Platine/Einzelschaltungen/Microcontoller.jpg" width="662" height="338">

## Hall-Effekt-Sensor

Der Hall-Effekt-Sensor misst das Magnetfeld des Magneten, der auf der Tür befestigt wird. Dadurch wird ermittelt ob die Tür gerade geöffnet oder geschlossen ist.
Wir haben den 49e verwendet, welcher ohne weitere Schaltung an den Microcontroller angeschlossen werden kann.

<img src="https://github.com/fabianhofmann03/EasyLock/blob/main/Schaltung%20&%20Platine/Einzelschaltungen/Hall_Sensor.jpg?raw=true" width="424" height="326">



# Protokoll

Das Projekt nutzt 4 verschiedene Nachrichten zum kommunizieren. Commands, Requests, Data und Status. 
Die ersten Bytes der Nachrichten bestehen aus den Indikatoren, die dem Programm sagen, was für eine Nachricht gesendet wurde. 
Am Ende jeder Nachricht wird ein Control Byte gesendet, das aus allen vorherigen Bytes der Nachricht, welche mit einer Bitweisen XOR Operation kombiniert wurden, besteht. 

## Command

| Command Protocol   |       |       |       |       |       |       |       |       |
|--------------------|-------|-------|-------|-------|-------|-------|-------|-------|
| Byte Name          | Bit 8 | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 |
| Command Start      | 1     | 0     | 0     | 0     | 0     | 0     | 0     | 1     |
| Command            | x     | x     | x     | x     | x     | x     | x     | x     |
| Control Byte       | x     | x     | x     | x     | x     | x     | x     | x     |

Der Command startet im Microcontroller eine Reihe an Befehlen die ausgeführt werden. Während des Befehls werden zwischen App und Microcontroller einige Request- und Data-Nachrichten ausgetauscht. Da der Microcontroller nur einen gewissen Speicherplatz hat, schickt dieser immer erst die Request, wenn er den Wert auch benötigt. Desshalb muss während dem Ausführen der Commands auch eine konstante Verbindung zwischen den Geräten herrschen.

Das zweite Byte besteht aus dem Command, der ausgeführt werden soll. Die Liste an ausführbaren Commands finden Sie hier:

| Command Byte    |       |       |       |       |       |       |       |       |                                             |
|-----------------|-------|-------|-------|-------|-------|-------|-------|-------|---------------------------------------------|
| Command         | Bit 8 | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 | Description                                 |
| Change Password | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 1     | Change the Password of the Lock             |
| Open Lock       | 0     | 0     | 0     | 0     | 0     | 0     | 1     | 0     | Open the Lock                               |
| Start Config    | 0     | 0     | 0     | 0     | 0     | 0     | 1     | 1     | Starts configuration of Hall Sensor         |

## Request und Data

| Request Protocol   |       |       |       |       |       |       |       |       |
|--------------------|-------|-------|-------|-------|-------|-------|-------|-------|
| Byte Name          | Bit 8 | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 |
| Request Start      | 0     | 1     | 0     | 0     | 0     | 0     | 1     | 0     |
| Requested Variable | x     | x     | x     | x     | x     | x     | x     | x     |
| Control Byte       | x     | x     | x     | x     | x     | x     | x     | x     |

Bei der Request wird von einem der Geräte ein Wert oder manchmal auch nur eine Status-Nachricht gewünscht.
Bei "Password" und "New Password" wird eine Data-Nachricht geschickt zurückgeschickt, bei "Lock Status" eine Status Nachricht.

Das zweite Byte besteht aus der Variable, die gewünscht wird. Dabei ist jedoch das höchstwertige Bit leer zu halten, da das das Flag Bit ist, das verwendet wird um zu erkennen, ob eine neue Request eingetroffen ist. Die Liste an Variablen finden am Ende dieses Abschnitts.

| Data Protocol      |       |       |       |       |       |       |       |       |
|--------------------|-------|-------|-------|-------|-------|-------|-------|-------|
| Byte Name          | Bit 8 | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 |
| Data Start         | 0     | 0     | 1     | 0     | 0     | 1     | 0     | 0     |
| Sent Variable      | x     | x     | x     | x     | x     | x     | x     | x     |
| Data Length        | x     | x     | x     | x     | x     | x     | x     | x     |
| Data               | x     | x     | x     | x     | x     | x     | x     | x     |
|                    | x     | x     | x     | x     | x     | x     | x     | x     |
|                    | ……    | ……    | ……    | ……    | ……    | ……    | ……    | ……    |
| Control Byte       | x     | x     | x     | x     | x     | x     | x     | x     |

Die Data-Nachricht versendet große Daten die für die Commads benötigt werden. Wegen des begrenzten Speichers des Microcontrollers wird die Länge auf 127 Bytes beschränkt. 

Im zweiten Byte ist gespeichert, welche Variable geschickt wurde. Wie bei der Request Nachricht ist das höchstwertige Bit leer zu halten. Die möglichen Variablen finden Sie am Ende dieses Abschnitts.
Im dritten Byte ist gespeichert, wie viele Bytes an Daten geschickt werden. 
In den folgenden Bits sind dann die Daten gespeichert.

| Variable Name |       |       |       |       |       |       |       |       |                              |
|---------------|-------|-------|-------|-------|-------|-------|-------|-------|------------------------------|
| Variable Name | Bit 8 | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 | Function                     |
| Password      | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 1     | Current Password of the lock |
| New Password  | 0     | 0     | 0     | 0     | 0     | 0     | 1     | 0     | New Password for the lock    |
| Lock Status   | 0     | 0     | 0     | 0     | 0     | 0     | 1     | 1     | Status of the Lock           |

## Status

| Status Protocol    |       |       |       |       |       |       |       |       |
|--------------------|-------|-------|-------|-------|-------|-------|-------|-------|
| Byte Name          | Bit 8 | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 |
| Status Start       | 0     | 0     | 0     | 1     | 1     | 0     | 0     | 0     |
| Status Message     | x     | x     | x     | x     | x     | x     | x     | x     |

Die Status Nachricht ist eine kurze Nachricht, welche keine spezielle Funktion hat. Beispiel hierfür sind Error-Messages, Bestätigungen für erhaltene Commands/Requests/Daten, oder auch aktuellen Status des Schlosses. 

Die Status-Nachricht hat kein Controll Byte, desshalb besteht sie nur aus dem Indikator und der Status Message.
Status-Messages die versendet werden können finden Sie hier: 

| Status Message    |       |       |       |       |       |       |       |       |                                                            |
|-------------------|-------|-------|-------|-------|-------|-------|-------|-------|------------------------------------------------------------|
| Status Name       | Bit 8 | Bit 7 | Bit 6 | Bit 5 | Bit 4 | Bit 3 | Bit 2 | Bit 1 | Description                                                |
| Command executed  | 0     | 0     | 0     | 0     | 0     | 0     | 0     | 1     | The wanted command is finished. No more information needed |
| Received          | 0     | 0     | 0     | 0     | 0     | 0     | 1     | 0     | Request or data received successfully                      |
| Deny              | 0     | 0     | 0     | 0     | 0     | 0     | 1     | 1     | Command denied                                             |
| Too much data     | 0     | 0     | 0     | 0     | 0     | 1     | 0     | 0     | Too much data was sent. Can't process                      |
| Message corrupted | 0     | 0     | 0     | 0     | 0     | 1     | 0     | 1     | Message was corrupted while sending. Please repeat         |
| Lock opened       | 0     | 0     | 0     | 0     | 0     | 1     | 1     | 0     | Lock is currently open                                     |
| Lock closed       | 0     | 0     | 0     | 0     | 0     | 1     | 1     | 1     | Lock is currently closed                                   |
| Cancel            | 0     | 0     | 0     | 0     | 1     | 0     | 0     | 0     | Cancel Command                                             |
| Continue          | 0     | 0     | 0     | 0     | 1     | 0     | 0     | 1     | Continue Command                                           |

