/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI', ['ngRoute', 'benchmarkServiceUI.controllers', 'benchmarkServiceUI.services', 'benchmarkServiceUI.filters',
                                          'ui.bootstrap', 'chart.js'])
        .config(['$routeProvider', function ($routeProvider) {
            $routeProvider
                .when('/', {
                    templateUrl: 'partials/mainPage.html',
                    controller: 'MainPageCtrl'
                })
                .when('/benchmark/:benchmarkName/:benchmarkSequenceId', {
                    templateUrl: 'partials/benchmarkRunPage.html',
                    controller: 'BenchmarkCtrl'
                })
                .when('/benchmark/:benchmarkName', {
                    templateUrl: 'partials/benchmarkPage.html',
                    controller: 'BenchmarkRunsCtrl'
                })
                .when('/environment/:environmentName', {
                    templateUrl: 'partials/environmentPage.html',
                    controller: 'EnvironmentCtrl'
                })
                .otherwise({
                    templateUrl: 'partials/404.html'
                });
        }]);
}());