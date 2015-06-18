/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.controllers', ['benchmarkServiceUI.services'])
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
                        return benchmarkRun.status === 'ENDED';
                    });

                    // query measurements graph data
                    var queryMeasurementKeys = _.uniq(_.flatten(_.map(benchmarkRuns,
                        function (benchmarkRun) {
                            return _.allKeys(benchmarkRun.queryAggregatedMeasurements);
                        }
                    )));

                    var queryMeasurementUnit = function (measurementKey) {
                        return benchmarkRuns[0].queryAggregatedMeasurements[measurementKey].unit;
                    };

                    var extractQueryAggregatedMeasurements = function (measurementKey) {
                        return _.map(benchmarkRuns, function (benchmarkRun) {
                            return benchmarkRun.queryAggregatedMeasurements[measurementKey];
                        });
                    };

                    var dataForQueryMeasurementKey = function (measurementKey) {
                        var aggregatedMeasurementsForKey = extractQueryAggregatedMeasurements(measurementKey);
                        return [
                            _.pluck(aggregatedMeasurementsForKey, 'mean'),
                            _.pluck(aggregatedMeasurementsForKey, 'min'),
                            _.pluck(aggregatedMeasurementsForKey, 'max'),
                            _.pluck(aggregatedMeasurementsForKey, 'stdDev')
                        ];
                    };

                    $scope.queryMeasurementGraphsData = _.map(queryMeasurementKeys, function (measurementKey) {
                        return {
                            name: measurementKey,
                            unit: queryMeasurementUnit(measurementKey),
                            data: dataForQueryMeasurementKey(measurementKey),
                            labels: _.pluck(benchmarkRuns, 'sequenceId'),
                            series: ['mean', 'min', 'max', 'stdDev']
                        }
                    });

                    // benchmark measurements graph data
                    var benchmarkMeasurementKeys = _.uniq(_.flatten(_.map(benchmarkRuns,
                        function (benchmarkRun) {
                            return _.pluck(benchmarkRun.measurements, 'name');
                        }
                    )));

                    var findBenchmarkMeasurement= function(benchmarkRun, measurementKey) {
                        return _.findWhere(benchmarkRun.measurements, { name: measurementKey });
                    }

                    var benchmarkMeasurementUnit = function (measurementKey) {
                        return findBenchmarkMeasurement(benchmarkRuns[0], measurementKey).unit;
                    };

                    var extractBenchmarkMeasurements = function (measurementKey) {
                        return _.map(benchmarkRuns, function (benchmarkRun) {
                            return findBenchmarkMeasurement(benchmarkRun, measurementKey);
                        });
                    };

                    var dataForBenchmarkMeasurementKey = function (measurementKey) {
                        var measurementsForKey = extractBenchmarkMeasurements(measurementKey);
                        return [
                            _.pluck(measurementsForKey, 'value')
                        ];
                    };

                    $scope.benchmarkMeasurementGraphsData = _.map(benchmarkMeasurementKeys, function (measurementKey) {
                        return {
                            name: measurementKey,
                            unit: benchmarkMeasurementUnit(measurementKey),
                            data: dataForBenchmarkMeasurementKey(measurementKey),
                            labels: _.pluck(benchmarkRuns, 'sequenceId'),
                            series: ['value']
                        }
                    });
                });
        }])
        .controller('BenchmarkCtrl', ['$scope', '$routeParams', '$modal', 'BenchmarkService', function ($scope, $routeParams, $modal, BenchmarkService) {
            BenchmarkService.loadBenchmarkRun($routeParams.benchmarkName, $routeParams.benchmarkSequenceId)
                .then(function (benchmarkRun) {
                    $scope.benchmarkRun = benchmarkRun;
                });

            $scope.benchmarkFromParam = function (benchmarkRun) {
                return benchmarkRun.started - 60 * 1000; // one minute before start
            };

            $scope.benchmarkToParam = function (benchmarkRun) {
                if (benchmarkRun.ended) {
                    return benchmarkRun.ended + 60 * 1000; // one minute after end
                }
                return Date.now();
            };

            $scope.measurementUnit = function (measurementKey) {
                return $scope.benchmarkRun.benchmarkMeasurements[measurementKey].unit;
            };

            $scope.showFailure = function (execution) {
                $modal.open({
                    templateUrl: 'partials/benchmarkRunErrorModal.html',
                    controller: 'BenchmarkRunErrorCtrl',
                    size: 'lg',
                    resolve: {
                        failure: function () {
                            return {
                                executionName: execution.name,
                                message: execution.attributes.failureMessage,
                                stackTrace: execution.attributes.failureStackTrace,
                                SQLErrorCode: execution.attributes.failureSQLErrorCode
                            };
                        }
                    }
                });
            };
        }])
        .controller('EnvironmentCtrl', ['$scope', '$routeParams', 'EnvironmentService', function ($scope, $routeParams, EnvironmentService) {
            EnvironmentService.loadEnvironment($routeParams.environmentName)
                .then(function (environment) {
                    $scope.environment = environment;
                });
        }])
        .controller('BenchmarkRunErrorCtrl', ['$scope', '$modalInstance', 'failure', function ($scope, $modalInstance, failure) {
            $scope.failure = failure;

            $scope.close = function () {
                $modalInstance.dismiss('cancel');
            }
        }]);
}());