const browser_obj = (typeof chrome !== "undefined") ? chrome : browser;
/** All of this must be stored inside the local storage and update with the contents from a secure repo */
const pem = "-----BEGIN RSA PRIVATE KEY-----MIICXAIBAAKBgQCxCqcTPq/a0Mte5K4LPyf5n9yj/aTCKxCk+iH+JrsgLE4UAuFAyJBnDKVrNCOIKnloNI+7tdgmXVjdux6MHg9czHMcNl/lsKpD/jyIaXLWGyAuKfhvcVkVqAsaelgoOi2k9Q/drkZLDkb7GSHcFfXkgZXCTTvUVkkLEQsPD94jdQIDAQABAoGAJeoFG776BB9g3kKU7z2oPvI4WzpPlXGJj/stWnNK8bnrQqfXO3t4SUlRB6NT1K5b77AJ9AYecyDcjierhBBP85DWRd72kjuIte58IEvHMnFG8ryS4zv0RcXrYvJf2w/7v0O11Bvo1tRQBnrby8xK3n0wZNUtN66a9ipw3OWosuUCQQDvpZU3xSOxiPUCOn63y8cTedH5kkzjkqMINWNNYd4rcKgvyefO89WiPzTCRKyE8wWGkeFFqlR3cmwTSMutOn8XAkEAvR9pmvy4zTnn7JgRGedxOduVosohewPeABptsMl3Ib92qTM2gHl192feEmK1rzRlnBiCGPybp0RpmFfhdn3pUwJAL9F1feRbY/B1GxW69Ue3GH7FVCxKJVq8J0Yn42f04ewf0zFRjO0AothD2cPEPN8VKi3vqmv7YL43LH3pDk7OzwJAZh1h7hdqyRTtDyiEg1IeJrlTsFQng75wzel3NK9zLbutnGpUkUYD1hQ1KgQ2SWWnP4NUK52phcVApss8p7gQlQJBAMu1pRb5z7cv/f83QdwqYYuRzoyWc5OxGQ8z+pMj5H5Is9mKWpJgTO0lmzMgJAGDwWnWbfpq6j/Qgdtg5LtfvmM=-----END RSA PRIVATE KEY-----";
// For the IV a simple edit to the agv client and to this extension is enough to concatenate key-iv, since iv has a fixed length
const iv = "AAAAAAAAAAAAAAAAAAAAAA==";
const pub = "-----BEGIN PUBLIC KEY-----MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCxCqcTPq/a0Mte5K4LPyf5n9yj/aTCKxCk+iH+JrsgLE4UAuFAyJBnDKVrNCOIKnloNI+7tdgmXVjdux6MHg9czHMcNl/lsKpD/jyIaXLWGyAuKfhvcVkVqAsaelgoOi2k9Q/drkZLDkb7GSHcFfXkgZXCTTvUVkkLEQsPD94jdQIDAQAB-----END PUBLIC KEY-----"
const sec_ks = {};
const index_hash = "";

let xml_http = new XMLHttpRequest();
xml_http.open("GET", domain);
xml_http.send();
xml_http.onreadystatechange = function() {
	if (this.readyState === 4 && this.status === 200) {
		// TODO : Check the integrity of the website using forge
		var sha256 = forge.md.sha256.create();
		sha256.update(forge.util.encode64(xml_http.responseText));
		console.log(sha256.digest().toHex());
		browser_obj.storage.local.set({'integrity':sha256.digest().toHex() === index_hash || true})
	}
}

const decrypts = function(records) {
	let cipher;
	let sec_k;
	for (let record of records) {
		sec_k = sec_ks[record.id];
		if (typeof sec_k === 'undefined') for (let secret of record.secrets) if (secret.name = 'admin') {
			sec_k = forge.pki.privateKeyFromPem(pem).decrypt(forge.util.decode64(secret.content.replace(/-/g,"+").replace(/_/g,"/")),'RSA-OAEP');
			sec_ks[record.id] = sec_k;
			break;
		}
		cipher = forge.cipher.createDecipher('AES-CBC', sec_k);
		cipher.start({iv: forge.util.decode64(iv)});
		cipher.update(forge.util.createBuffer(forge.util.decode64(record.content.replace(/-/g,"+").replace(/_/g,"/"))));
		cipher.finish();
		console.log(cipher.output.data);
		console.log("");
		document.querySelector("#"+record.client.name+"_"+record.id+" div.content").innerHTML = "\n\r"+cipher.output.data+"\n\r";
	}
}
const decrypt = function(record) {
	cipher = forge.cipher.createDecipher('AES-CBC', sec_ks[record.id]);
	cipher.start({iv: forge.util.decode64(iv)});
	cipher.update(forge.util.createBuffer(forge.util.decode64(record.content.replace(/-/g,"+").replace(/_/g,"/"))));
	cipher.finish();
	console.log(cipher.output.data);
	console.log("");
}

window.addEventListener("message", function(event) {
    // Only accept messages from himself
    if (event.source != window)
		return;
    if (typeof event.data !== 'undefined' && event.data.type === 'FROM_PAGE')
		if (typeof event.data.text !== 'undefined')
			decrypt(event.data.text);
		else if (typeof event.data.list !== 'undefined')
			decrypts(event.data.list);
});
