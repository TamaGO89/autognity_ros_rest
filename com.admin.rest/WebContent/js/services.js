'use strict';

/**-----------------------------------------------------------------------------------------------------------------------------
 * SESSION SERVICE : Enhanced resource service, manage the paths, the authorization header, etc.
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.service('$session', ['$resource','$cookies','URL','$q','$rootScope',
					 function($resource , $cookies , URL , $q , $rootScope) {

	// Instance variables
	const svc = this;
	svc.err = { auth : [401,403],
				std : [404,405],
				base : [400],
				server : [500,501] };
	svc.local = URL;

	// Set the authorization header
	svc.setAuth = function(username, password = '') {
		svc.auth = { Authorization: password.length > 0 ? URL.bsc + ' ' + btoa(username + ':' + password)
														: URL.otp + ' ' + btoa(username) }; };

	// Get all the paths the user is allowed to access to from the server, used to translate the requests from the client
	svc.getPaths = function() {
		return $q(function (resolve, reject) {
			if (typeof svc.paths === 'undefined') {
				$resource(URL.base + URL.paths, {}, svc.header('GET') ).query( {},
					function(data) { console.log(data); resolve(svc.paths=data); },
					function(err) { reject(svc.except(err,{})); });
			} else resolve(svc.paths); }); };
	svc.isPath = function(path) { return $q( function (resolve) {
		if (typeof svc.local[path] !== 'undefined') resolve(true);
		else svc.getPaths().then( function(paths) { resolve(typeof paths[path] !== 'undefined'); }); }); };
	svc.getPath = function(path) { return $q( function (resolve) {
		if (typeof svc.local[path] !== 'undefined') resolve(svc.local[path]);
		else svc.getPaths().then( function(paths) { resolve(paths[path]); }); }); };

	// Manage the exception, if the client failed to certify the user navigate to the login screen 
	svc.except = function(err, args) {
		if (args.debug !== false) console.debug(err);
		if (svc.err.auth.indexOf(err.status) >= 0) {
			if (args.token !== false) $cookies.remove('Token');
			$rootScope.$broadcast('navigate', {path : typeof args.path === 'string' ? args.path : '/login'}); } };

	// http GET method
	svc.get = function(path, param = {}, args = {}, exc = svc.except) {
		return $q(function (resolve, reject) {
			svc.getPath(path).then(function(p) {
				$resource(URL.base + p, {}, svc.header('GET') ).query(
					param, function(d) { resolve(d); }, function(e) { reject(exc(e,args)); }); }); }); };

	// http POST method
	svc.post = function(path, data, param = {}, args = {}, exc = svc.except) {
		return $q(function (resolve, reject) {
			svc.getPath(path).then(function(p) {
				$resource(URL.base + p, {}, svc.header('POST') ).query(
					param, data, function(d) { resolve(d); }, function(e) { reject(exc(e,args)); }); }); }); };

	// Return the right query, with the authorization header if there's no valid cookie for the authentication
	svc.header = function(mode) {
		return $cookies.get('Token') ? {query:{method:mode}} : {query:{method:mode,headers:svc.auth}}; };
}]);
