/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.controllers', ['benchmarkServiceUI.service'])
        .controller('MainPageCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {
            $scope.latestBenchmarks = [];
            $scope.pageSize = $routeParams.size ? $routeParams.size : 10;
            $scope.page = $routeParams.page ? $routeParams.page : 0;

            BenchmarkService.loadLatestBenchmarks($scope.page, $scope.pageSize)
                .then(function (latestBenchmarks) {
                    $scope.latestBenchmarks = latestBenchmarks;
                });
        }])
        .controller('BenchmarkRunsCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {
            $scope.benchmarkRuns = [];
            $scope.benchmarkName = $routeParams.benchmarkName;
            $scope.pageSize = $routeParams.size ? $routeParams.size : 10;
            $scope.page = $routeParams.page ? $routeParams.page : 0;

            BenchmarkService.loadBenchmarkRuns($routeParams.benchmarkName, $scope.page, $scope.pageSize)
                .then(function (benchmarkRuns) {
                    $scope.benchmarkRuns = benchmarkRuns;
                });
        }])
        .controller('BenchmarkCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {
            $scope.benchmark = {};

            var loadBenchmark = function () {
                BenchmarkService.loadBenchmark($routeParams.benchmarkName, $routeParams.benchmarkSequenceId)
                    .then(function (benchmark) {
                        $scope.benchmark = benchmark;
                    });
            };

            loadBenchmark();
        }]);
}());