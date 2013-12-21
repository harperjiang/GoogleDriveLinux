GoogleDriveLinux
================

This is a Google Drive Client for Linux written in Java.

To start using this client, simply modify org.harper.driveclient.Configuration to indicate the root folder of Google Drive, and execute org.harper.driveclient.Main.

The program will create a ".google_drive_client" folder under the home directory, then open a browser asking you to allow this program to access your Google Drive files. The authentication info will be stored in that folder. To change account, delete that folder and restart the program to login with another Google Account.


