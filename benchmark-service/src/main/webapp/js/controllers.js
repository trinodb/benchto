/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.controllers', ['benchmarkServiceUI.service'])
        .controller('MainPageCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {
            $scope.pageSize = $routeParams.size ? $routeParams.size : 20;
            $scope.page = $routeParams.page ? $routeParams.page : 0;

            BenchmarkService.loadLatestBenchmarkRuns($scope.page, $scope.pageSize)
                .then(function (latestBenchmarkRuns) {
                    $scope.latestBenchmarkRuns = latestBenchmarkRuns;
                });
        }])
        .controller('BenchmarkRunsCtrl', ['$scope', '$routeParams', '$location', 'BenchmarkService', function ($scope, $routeParams, $location, BenchmarkService) {
            $scope.pageSize = $routeParams.size ? $routeParams.size : 20;
            $scope.page = $routeParams.page ? $routeParams.page : 0;

            $scope.onBenchmarkClick = function (points, evt) {
                if (points) {
                    var benchmarkRunSequenceId = points[0].label;
                    $location.path('benchmark/' + $routeParams.benchmarkName + '/' + benchmarkRunSequenceId);
                }
            };

            BenchmarkService.loadBenchmark($routeParams.benchmarkName, $scope.page, $scope.pageSize)
                .then(function (benchmark) {
                    $scope.benchmark = benchmark;
                    // filter out benchmark runs which have not finished
                    var benchmarkRuns = _.filter(benchmark.runs, function (benchmarkRun) {
                        return !angular.equals({}, benchmarkRun.aggregatedMeasurements);
                    });

                    var measurementKeys = _.uniq(_.flatten(_.map(benchmarkRuns,
                        function (benchmarkRun) {
                            return _.allKeys(benchmarkRun.aggregatedMeasurements);
                        }
                    )));

                    var measurementUnit = function (measurementKey) {
                        return benchmarkRuns[0].aggregatedMeasurements[measurementKey].unit;
                    };

                    var extractAggregatedMeasurements = function (measurementKey) {
                        return _.map(benchmarkRuns, function (benchmarkRun) {
                            return benchmarkRun.aggregatedMeasurements[measurementKey];
                        });
                    };

                    var dataFor = function (measurementKey) {
                        var aggregatedMeasurementsForKey = extractAggregatedMeasurements(measurementKey);
                        return [
                            _.pluck(aggregatedMeasurementsForKey, 'mean'),
                            _.pluck(aggregatedMeasurementsForKey, 'min'),
                            _.pluck(aggregatedMeasurementsForKey, 'max'),
                            _.pluck(aggregatedMeasurementsForKey, 'stdDev')
                        ];
                    };

                    $scope.measurementGraphsData = _.map(measurementKeys, function (measurementKey) {
                        return {
                            name: measurementKey,
                            unit: measurementUnit(measurementKey),
                            data: dataFor(measurementKey),
                            labels: _.pluck(benchmarkRuns, 'sequenceId'),
                            series: ['mean', 'min', 'max', 'stdDev']
                        }
                    });
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

            $scope.measurementUnit = function (measurementKey) {
                return $scope.benchmarkRun.aggregatedMeasurements[measurementKey].unit;
            }
        }]);
}());