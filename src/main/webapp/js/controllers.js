/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.controllers', ['benchmarkServiceUI.service'])
        .controller('MainPageCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {
            $scope.pageSize = $routeParams.size ? $routeParams.size : 10;
            $scope.page = $routeParams.page ? $routeParams.page : 0;

            BenchmarkService.loadLatestBenchmarkRuns($scope.page, $scope.pageSize)
                .then(function (latestBenchmarkRuns) {
                    $scope.latestBenchmarkRuns = latestBenchmarkRuns;
                });
        }])
        .controller('BenchmarkRunsCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {
            $scope.pageSize = $routeParams.size ? $routeParams.size : 10;
            $scope.page = $routeParams.page ? $routeParams.page : 0;

            BenchmarkService.loadBenchmark($routeParams.benchmarkName, $scope.page, $scope.pageSize)
                .then(function (benchmark) {
                    $scope.benchmark = benchmark;
                });
        }])
        .controller('BenchmarkCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {
            var loadBenchmark = function () {
                BenchmarkService.loadBenchmarkRun($routeParams.benchmarkName, $routeParams.benchmarkSequenceId)
                    .then(function (benchmarkRun) {
                        $scope.benchmarkRun = benchmarkRun;
                    });
            };

            loadBenchmark();
        }]);
}());