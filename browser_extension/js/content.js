'use strict';

/** All of this must be stored inside the local storage and update with the contents from a secure repo */
const pem = "-----BEGIN RSA PRIVATE KEY-----MIICXAIBAAKBgQCxCqcTPq/a0Mte5K4LPyf5n9yj/aTCKxCk+iH+JrsgLE4UAuFAyJBnDKVrNCOIKnloNI+7tdgmXVjdux6MHg9czHMcNl/lsKpD/jyIaXLWGyAuKfhvcVkVqAsaelgoOi2k9Q/drkZLDkb7GSHcFfXkgZXCTTvUVkkLEQsPD94jdQIDAQABAoGAJeoFG776BB9g3kKU7z2oPvI4WzpPlXGJj/stWnNK8bnrQqfXO3t4SUlRB6NT1K5b77AJ9AYecyDcjierhBBP85DWRd72kjuIte58IEvHMnFG8ryS4zv0RcXrYvJf2w/7v0O11Bvo1tRQBnrby8xK3n0wZNUtN66a9ipw3OWosuUCQQDvpZU3xSOxiPUCOn63y8cTedH5kkzjkqMINWNNYd4rcKgvyefO89WiPzTCRKyE8wWGkeFFqlR3cmwTSMutOn8XAkEAvR9pmvy4zTnn7JgRGedxOduVosohewPeABptsMl3Ib92qTM2gHl192feEmK1rzRlnBiCGPybp0RpmFfhdn3pUwJAL9F1feRbY/B1GxW69Ue3GH7FVCxKJVq8J0Yn42f04ewf0zFRjO0AothD2cPEPN8VKi3vqmv7YL43LH3pDk7OzwJAZh1h7hdqyRTtDyiEg1IeJrlTsFQng75wzel3NK9zLbutnGpUkUYD1hQ1KgQ2SWWnP4NUK52phcVApss8p7gQlQJBAMu1pRb5z7cv/f83QdwqYYuRzoyWc5OxGQ8z+pMj5H5Is9mKWpJgTO0lmzMgJAGDwWnWbfpq6j/Qgdtg5LtfvmM=-----END RSA PRIVATE KEY-----";
// For the IV a simple edit to the agv client and to this extension is enough to concatenate key-iv, since iv has a fixed length
const iv = "AAAAAAAAAAAAAAAAAAAAAA==";
const pub = "-----BEGIN PUBLIC KEY-----MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCxCqcTPq/a0Mte5K4LPyf5n9yj/aTCKxCk+iH+JrsgLE4UAuFAyJBnDKVrNCOIKnloNI+7tdgmXVjdux6MHg9czHMcNl/lsKpD/jyIaXLWGyAuKfhvcVkVqAsaelgoOi2k9Q/drkZLDkb7GSHcFfXkgZXCTTvUVkkLEQsPD94jdQIDAQAB-----END PUBLIC KEY-----";
const index_hash = "";
let local_username;
let local_password;
let local_priv_key;


/** constant values */
// Browser reference, domain name, rest service, initial resources
const browser_obj = (typeof chrome !== "undefined") ? chrome : browser;
const domain = "http://localhost:8080/com.admin.rest";
const rest_path = "/ros_rest";
const list_path = "/log/paths"
// values to check and generate private, public and master keys
const key_min_len = 8;
const key_sim_len = 4;
const key_regex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[_/+-])[A-Za-z0-9_/+-]{8,16}$";
const key_length = 32;
const key_iter = 45000;
const pubkey_exp = 0x101;
// dictionary for secret keys already decrypted
const sec_ks = {};

/** Check string similarity */
const similar = function(string1, string2, len) {
	let s1 = string1.toLowerCase();
	let s2 = string2.toLowerCase();
	for (let i = 0; i < s2.length - len + 1; i++) if (s1.indexOf(s2.substr(i,len)) >= 0) return true;
	return false; };

/** setup a master key, populate the local storage with the encrypted values */
const set_key = function(key, user, pass) {
	return new Promise(function (resolve, reject) {
		// check if the master_key format is incorrect
		if (key.length < key_min_len || key.match(key_regex) === null ||
			similar(key, user, key_sim_len) || similar(key, pass, key_sim_len))
			reject("the master_key is unacceptable");
		// Generate a key pair, try to upload it with given username and password
		let keypair = forge.pki.rsa.generateKeyPair({bits: 2048, e: pubkey_exp});
		var request = new XMLHttpRequest();
		request.open("GET", domain + rest_path + list_path);
		request.setRequestHeader("Authorization", "Basic " + btoa(user + ':' + pass));
		request.send();
		// The real path to the resources is retrieved from the website, check if the user is allowed to set his key
		request.onreadystatechange = function() {
			if (this.readyState === 4 && this.status === 200) {
				let paths = request.responseText;
				request = new XMLHttpRequest();
				request.open("POST", domain + rest_path + JSON.parse(paths).keys);
				request.setRequestHeader("Content-Type", "application/json");
				request.send(JSON.stringify({content: forge.pki.publicKeyToPem(keypair.publicKey)}));
				// POST the public key, if everything went according to plan save everything in the local storage
				request.onreadystatechange = function() {
					console.log("risposta della chiave arrivata");
					//if (this.readyState === 4 && this.status === 201) {
						let salt = forge.random.getBytesSync(128);
						forge.pkcs5.pbkdf2(key, salt, key_iter, 32, function(err, secret_key) {
							let cipher = forge.cipher.createCipher('AES-CBC', secret_key);
							cipher.start({iv: atob(iv.replace(/-/g,"+").replace(/_/g,"/"))});
							cipher.update(forge.util.createBuffer(user));
							cipher.finish();
							browser_obj.storage.local.set({'username' : btoa(cipher.output.data)});
							cipher = forge.cipher.createCipher('AES-CBC', secret_key);
							cipher.start({iv: atob(iv.replace(/-/g,"+").replace(/_/g,"/"))});
							cipher.update(forge.util.createBuffer(pass));
							cipher.finish();
							browser_obj.storage.local.set({'password' : btoa(cipher.output.data)});
							cipher = forge.cipher.createCipher('AES-CBC', secret_key);
							cipher.start({iv: atob(iv.replace(/-/g,"+").replace(/_/g,"/"))});
							cipher.update(forge.util.createBuffer(forge.pki.privateKeyToPem(keypair.privateKey)));
							cipher.finish();
							browser_obj.storage.local.set({'priv_key' : btoa(cipher.output.data)});
							browser_obj.storage.local.set({'key_salt' : salt});
							resolve(key);
						});
						 };
					//} else reject("user and password are correct, but you can't set a public key"); };
			} else reject("user and password are incorrect"); }; }); };

const get_key = function(key) {
	return new Promise(function (resolve, reject) {
		browser_obj.storage.local.get(['key_salt', 'username', 'password', 'priv_key'], function(result) {
			let master_key = forge.pkcs5.pbkdf2(key, result.key_salt, key_iter, 32);
			let cipher = forge.cipher.createDecipher('AES-CBC', master_key);
			cipher.start({iv: atob(iv.replace(/-/g,"+").replace(/_/g,"/"))});
			cipher.update(forge.util.createBuffer(atob(result.username)));
			cipher.finish();
			local_username = cipher.output.data;
			cipher = forge.cipher.createDecipher('AES-CBC', master_key);
			cipher.start({iv: atob(iv.replace(/-/g,"+").replace(/_/g,"/"))});
			cipher.update(forge.util.createBuffer(atob(result.password)));
			cipher.finish();
			local_password = cipher.output.data;
			cipher = forge.cipher.createDecipher('AES-CBC', master_key);
			cipher.start({iv: atob(iv.replace(/-/g,"+").replace(/_/g,"/"))});
			cipher.update(forge.util.createBuffer(atob(result.priv_key)));
			cipher.finish();
			local_priv_key = cipher.output.data;
			resolve({username:local_username, password:local_password, priv_key:local_priv_key});
			reject('i sincerely don\'t know what happened'); }); }); };

/** Decrypt records */
const decrypt = function(record) {
	let cipher = forge.cipher.createDecipher('AES-CBC', sec_ks[record.id]);
	cipher.start({iv: atob(iv.replace(/-/g,"+").replace(/_/g,"/"))});
	cipher.update(forge.util.createBuffer(atob(record.content.replace(/-/g,"+").replace(/_/g,"/"))));
	cipher.finish();
	console.log(cipher.output.data);
	console.log("");
	return cipher.output.data;
};

const decrypts = function(records) {
	let plain_text;
	browser_obj.storage.local.get(['priv_key'], function(result) {
		if (typeof local_priv_key === 'undefined') return;
		let priv_key = forge.pki.privateKeyFromPem(local_priv_key);
		for (let record of records) {
			if (typeof sec_ks[record.id] === 'undefined')
				sec_ks[record.id] = priv_key.decrypt(atob(record.secrets[0].content.replace(/-/g,"+").replace(/_/g,"/")),'RSA-OAEP');
			document.querySelector("#"+record.client.name+"_"+record.id+" div.content").innerHTML="\n\r"+decrypt(record)+"\n\r";
		} }); };

window.addEventListener("message", function(event) {
    // Only accept messages from himself
	if (!(event.source == window)) return;
	if (typeof event.data !== 'undefined' && event.data.type === 'FROM_PAGE')
		if (typeof event.data.text !== 'undefined') decrypt(event.data.text);
		else if (typeof event.data.list !== 'undefined') decrypts(event.data.list); });
			
browser_obj.runtime.onMessage.addListener(function(request, sender, sendResponse) {
	if (request.type === 'FROM_POPUP')
		if (typeof request.user === 'undefined') get_key(atob(request.key)).then(function(result) { return result; });
		else set_key(atob(request.key), atob(request.user), atob(request.pass)).then(function(result) { return result; }); });

/** Check the website integrity */
let xml_http = new XMLHttpRequest();
xml_http.open("GET", domain);
xml_http.send();
xml_http.onreadystatechange = function() {
	if (this.readyState === 4 && this.status === 200) {
		var sha256 = forge.md.sha256.create();
		sha256.update(btoa(xml_http.responseText));
		console.log(sha256.digest().toHex());
		browser_obj.storage.local.set({'integrity':sha256.digest().toHex() === index_hash || true}) } };
