'use strict';

/**-----------------------------------------------------------------------------------------------------------------------------
 * MAIN CONTROLLER : controls header and footer, initialize the session and manage the navigation on the webapp
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.controller('ctrl', ['$rootScope','$session','$location','UTILS',
				   function( $rootScope , $session , $location , UTILS ) {

	// Instance variables
	const ctrl = this;
	ctrl.menu =[{ id : 'main', name : 'Main', path : '/' },
				{ id : '1', name : 'First', path : '/1' },
				{ id : '2', name : 'Second', path : '/2' },
				{ id : '3', name : 'Third', path : '/3' },
				{ id : 'log', name : 'Login', path : '/login' }];

	$rootScope.session = $session;
	$rootScope.location = $location;
	$rootScope.history = ['/'];
	$rootScope.browser = (typeof chrome !== 'undefined') ? chrome : browser;
	$rootScope.window = window;
	/**
	var evt = document.createEvent("CustomEvent");
	evt.initCustomEvent("eventname", true, true, {callback:callbackstring});
	window.dispatchEvent(evt);*/

	// Update the location, the history and the active element of the header
	ctrl.set_path = function(path) {
		if (typeof path.id === 'string') return path;
		for (let i = 1; i < ctrl.menu.length; i++) if (path.path.startsWith(ctrl.menu[i].path)) {
			path.id = ctrl.menu[i].id; return path; }
		path.id = ctrl.menu[0].id; return path; };
	ctrl.navigate = function(path) {
		ctrl.active = path.id;
		let backup = typeof path.backup === 'string' ? path.backup : $rootScope.location.path();
		if (backup.indexOf('/login') < 0) $rootScope.history.push(backup);
		if ($rootScope.history.length === 0) $rootScope.history.push('/');
		if ($rootScope.history.length > UTILS.history)
			$rootScope.history.splice(0,$rootScope.history.length - UTILS.history);
		$rootScope.location.path(path.path); };

	// Listen on navigation events and forward them
	$rootScope.$on('navigate', function(event, path) {
		ctrl.navigate(ctrl.set_path(path)); });
	$rootScope.$on('backward', function(event) {
		ctrl.navigate(ctrl.set_path({path:$rootScope.history.pop()})); });
	$rootScope.$on('add_elem', function(event, elem) {
		ctrl.menu.push(elem);
	});
}]);

/**-----------------------------------------------------------------------------------------------------------------------------
 * CONTROLLER :
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.controller('ctrl0', ['$rootScope',
					 function($rootScope) {

	const ctrl = this;

	ctrl.get = function(path) {
		return $rootScope.session.get(path).then(function(result) { ctrl.message = result; }); };
	ctrl.get('client');
}]);

/**-----------------------------------------------------------------------------------------------------------------------------
 * CONTROLLER :
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.controller('ctrl1', ['$rootScope',
					 function($rootScope) {

	const ctrl = this;

	ctrl.get = function(path) {
		return $rootScope.session.get(path).then(function(result) { ctrl.message = result; }); };
	ctrl.get('clients');
}]);

/**-----------------------------------------------------------------------------------------------------------------------------
 * CONTROLLER :
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.controller('ctrl2', ['$rootScope',
					 function($rootScope) {

	const ctrl = this;

	ctrl.get = function(path) {
		return $rootScope.session.get(path).then(function(result) { ctrl.message = result; }); };
	ctrl.get('stations');
}]);

/**-----------------------------------------------------------------------------------------------------------------------------
 * CONTROLLER :
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.controller('ctrl3', ['$rootScope',
					 function($rootScope) {
	
	const ctrl = this;
	
	ctrl.get = function(path) {
		return $rootScope.session.get(path).then(function(result) { ctrl.message = result; }); };
}]);

/**-----------------------------------------------------------------------------------------------------------------------------
 * NODE CONTROLLER : Manage the side menu with stations and users controlled by the client
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.controller("main_ctrl", ['$scope','$rootScope',
						function( $scope , $rootScope ) {

	// Instance variables
	const ctrl = this;
	ctrl.search = '';

	// toggle the visibility of the tree nodes
    ctrl.toggle = function(item) {
    	item.hide = !item.hide; };

    // Populate the tree with a given client, beware of duplicates
    ctrl.populate = function(nodes, client) {
    	for (let node of nodes) {
			if (client.stations.indexOf(node.name) >= 0) {
				if (typeof node.elem === 'undefined') node.elem = [];
				node.elem.push(client); }
    		if (node.sub.length > 0)
    			ctrl.populate(node.sub, client); }};

    // Update the visibility of the elements in the tree with the search filter
    ctrl.update = function() {
    	for (let r of ctrl.robots) r.hide = r.client.name.toLowerCase().indexOf(ctrl.search.toLowerCase()) < 0;
		for (let u of ctrl.users) u.hide = u.client.name.toLowerCase().indexOf(ctrl.search.toLowerCase()) < 0;
	};

    ctrl.set_visible = function(item) {
    	if (typeof item.config === 'undefined' || typeof item.infos === 'undefined')
    		$rootScope.session.get('overview',{rob : item.client.name}).then(function(res) {
    			item.config = res.content;
    			item.infos = res.list;
    			$rootScope.window.postMessage({type:'FROM_PAGE', list:res.list}, "*");
    		})
    	ctrl.visible = item;
	};
	
	ctrl.download = function(item) {
		console.log("vuoi scaricare questo ogetto "+item.id);
		$rootScope.session.get('raw', {id:item.id}).then(function(raw) {
			console.log("this is the content to decrypt");
			console.log(raw);
			$rootScope.window.postMessage({type:'FROM_PAGE', text:raw}, "*");
		})
	}

	// At controller initialization, download stations and clients, populate the tree
	$rootScope.session.get('stations').then(function(sta) {
		ctrl.stations = sta.list;
		$rootScope.session.get('robots').then(	function(rob) {
			ctrl.robots = rob.list;
			for (let r of ctrl.robots) {
				r.hide = r.client.name.toLowerCase().indexOf(ctrl.search.toLowerCase()) < 0;
				ctrl.populate(ctrl.stations, r);
			}});
		$rootScope.session.get('clients').then(	function(usr) {
			ctrl.users = usr.list;
			for (let u of ctrl.users) {
				u.hide = u.client.name.toLowerCase().indexOf(ctrl.search.toLowerCase()) < 0;
				ctrl.populate(ctrl.stations, u);
			}}); });
}]);

/**-----------------------------------------------------------------------------------------------------------------------------
 * LOGIN CONTROLLER : Allow the user to insert username and password to login into the webapp
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.controller('login_ctrl', ['$rootScope',
						 function( $rootScope ) {

	// instance variables
	const ctrl = this;
	ctrl.show = 0;

	ctrl.toggle = function(index) { ctrl.show = index; };
	ctrl.signin = function() {
		$rootScope.session.post('signin', {name:ctrl.s_public, address:ctrl.s_address,
										   firstname:ctrl.s_firstname, lastname:ctrl.s_lastname}).then(
			function(result) { ctrl.s_message = 'check your email account to complete the registration'; }).catch(
			function(error) { ctrl.s_alert = error; }); };
	ctrl.activate = function() {
		if (ctrl.a_password !== ctrl.a_password2) ctrl.a_alert = 'the two password fields are different';
		else $rootScope.session.post('activate', {username:ctrl.a_username, password:ctrl.a_password}).then(
			function(result) { ctrl.a_message = 'your account is now activated, store user and pass in a safe place'}).catch(
			function(error) { ctrl.a_alert = error; }); };
	// Set the authorization header and go back to the previous page (otherwise it goes to the main page)
	ctrl.login = function() {
		if (typeof ctrl.l_username === 'undefined' || typeof ctrl.l_password === 'undefined') return;
		$rootScope.session.setAuth(ctrl.l_username, ctrl.l_password);
		$rootScope.$broadcast('backward'); };
	ctrl.verify = function() {
		if (typeof ctrl.v_onetimepass === 'undefined') return;
		$rootScope.session.setAuth(ctrl.v_onetimepass);
		$rootScope.session.get('temporary').then(
			function(result) {
				ctrl.show = 3;
				ctrl.temporary = result; }).catch(
			function(error) { ctrl.v_alert = 'the OneTimePassword is wrong'; }); };
}]);
