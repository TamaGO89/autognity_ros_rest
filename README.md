# ros_rest

Content of the repository:

- browser_extension : contains the cross browser extension, it's unpacked and must be loaded manually
- com.admin.rest : contains the restful web service code, runs with apache tomcat 8.0
- ros_rest : it's the node for ROS that runs on the AGVs
- database.sql : the database with minimal initial setup: placeholder for the administrator account, initial roles and stations

Setup the DB and start the server then, to activate the administrator account (admin), log into the website at http://localhost/com.admin.rest
Login -> verify -> OTP = QROG86 -> set Username and Password
