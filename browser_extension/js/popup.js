const browser_obj = (typeof chrome !== "undefined") ? chrome : browser;

if (true) {
	browser_obj.tabs.query({active: true, currentWindow: true}, function(tabs) {
		browser_obj.tabs.sendMessage(tabs[0].id, {type:"getText"}, function(response){
			document.querySelector("#text").innerText = response;
		});
	});
}
