const browser_obj = (typeof chrome !== "undefined") ? chrome : browser;
const domain = "http://localhost:8080/com.admin.rest/";
const index_hash = "";
const safe_to_go = function() {
	browser_obj.tabs.getSelected(null, function(tab) {
		if (tab.url.indexOf(domain) >= 0) {
			let xml_http = new XMLHttpRequest();
			xml_http.open("GET", domain);
			xml_http.send();
			xml_http.onreadystatechange = function() {
				if (this.readyState === 4 && this.status === 200) {
					// TODO : Check the integrity of the website using forge
					var sha256 = forge.md.sha256.create();
					sha256.update(forge.util.encode64(xml_http.responseText));
					alert(sha256.digest().toHex());
					console.log(sha256.digest());
					if (sha256.digest() === index_hash) {
						console.log("il digest corrisponde");
						return true;
					}
				}
			}
		}
	});
	console.log("il digest non corrisponde");
	return false;
}
