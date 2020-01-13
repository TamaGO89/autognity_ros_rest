# ros_rest

Content of the repository:

- browser_extension : contains the cross browser extension, it's unpacked and must be loaded manually
- com.admin.rest : contains the restful web service code, runs with apache tomcat 8.0
- ros_rest : it's the node for ROS that runs on the AGVs
- database.sql : the database with minimal initial setup: placeholder for the administrator account, initial roles and stations

Setup the DB and start the server then...

- To activate the administrator account (admin)
Log into the website at http://localhost/com.admin.rest
Login -> verify -> OTP = QROG86 -> set Username and Password
- To upload the public key of the administrator (already setup into the browser extension)
POST with basic auth to http://localhost:8080/com.admin.rest/ros_rest/key/key

{
	"name":"admin",
	"content":"-----BEGIN PUBLIC KEY-----MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCxCqcTPq/a0Mte5K4LPyf5n9yj/aTCKxCk+iH+JrsgLE4UAuFAyJBnDKVrNCOIKnloNI+7tdgmXVjdux6MHg9czHMcNl/lsKpD/jyIaXLWGyAuKfhvcVkVqAsaelgoOi2k9Q/drkZLDkb7GSHcFfXkgZXCTTvUVkkLEQsPD94jdQIDAQAB-----END PUBLIC KEY-----"
}

- Right now the private key is stored in plain text inside the browser extension, I'm working on a simple method to store it inside the local storage encrypted with a master_key, along the username and the password of the user
- The virtual machine needed to run the agv client is to big to be shared on my local storage, please call me and we can find a solution to share it
