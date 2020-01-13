'use strict';

const rosRest = angular.module('RosRest', ['ngResource','ngCookies','ngRoute']);

/**-----------------------------------------------------------------------------------------------------------------------------
 * CONFIGURATION : Set the constant dictionaries, route provider, exception interceptor, etc...
 *----------------------------------------------------------------------------------------------------------------------------*/
rosRest.config(['$provide','$cookiesProvider','$routeProvider','$httpProvider',
		function($provide , $cookiesProvider , $routeProvider , $httpProvider){

	// Dictionaries
	$provide.constant('APP', {name : 'RosRest', ver : 1.0});
	$provide.constant('URL', {bsc: 'Basic', otp: 'OneTimePass', base: 'ros_rest', paths: '/log/paths', signin: '/log/signin'});
	$provide.constant('UTILS', {history : 5});

	// Exception manager for requests
	$httpProvider.interceptors.push(function ($q,$rootScope) { return function (responseError) {
		console.debug($rootScope.error = responseError); return $q.reject(responseError); }; });

	// Default path for the cookies the app will have to manage
	$cookiesProvider.defaults = { path: '/com.admin.rest' };

	// Route provider: template and controller informations for routing
	$routeProvider
		.when('/', { templateUrl : 'templates/main.html', controller : 'main_ctrl', controllerAs : 'main_ctrl' })
		.when('/1', { templateUrl : 'templates/1.html', controller : 'ctrl1', controllerAs : 'ctrl1' })
		.when('/2', { templateUrl : 'templates/2.html', controller : 'ctrl2', controllerAs : 'ctrl2' })
		.when('/3', { templateUrl : 'templates/main.html', controller : 'main_ctrl', controllerAs : 'main_ctrl' })
		.when('/login', { templateUrl : 'templates/login.html', controller : 'login_ctrl', controllerAs : 'login_ctrl' })
		.otherwise( { redirectTo : '/' });
}]);
