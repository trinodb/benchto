/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.controllers', ['benchmarkServiceUI.services', 'nvd3'])
        .controller('BenchmarkListCtrl', ['$scope', '$routeParams', 'BenchmarkService', function ($scope, $routeParams, BenchmarkService) {

            BenchmarkService.loadLatestBenchmarkRuns()
                .then(function (latestBenchmarkRuns) {
                    $scope.availableVariables = _.chain(latestBenchmarkRuns)
                        .map(function (benchmarkRun) { return _.keys(benchmarkRun.variables); })
                        .flatten()
                        .uniq()
                        .map(function (variableName) { return {name: variableName, visible: false}})
                        .value();

                    // first three columns will be visible
                    for (var i = 0; i < Math.max(4, $scope.availableVariables.length); ++i) {
                        $scope.availableVariables[i].visible = true;
                    }
                    $scope.latestBenchmarkRuns = latestBenchmarkRuns;
                });
        }])
        .controller('BenchmarkCtrl', ['$scope', '$routeParams', '$location', '$filter', 'BenchmarkService', function ($scope, $routeParams, $location, $filter, BenchmarkService) {
            $scope.uniqueName = $routeParams.uniqueName;

            $scope.onBenchmarkClick = function (points, evt) {
                if (points) {
                    var benchmarkRunSequenceId = points[0].label;
                    $location.path('benchmark/' + $routeParams.uniqueName + '/' + benchmarkRunSequenceId);
                }
            };

            BenchmarkService.loadBenchmark($routeParams.uniqueName)
                .then(function (runs) {
                    $scope.benchmarkRuns = runs;
                    // filter out benchmark runs which have not finished
                    var benchmarkRuns = _.filter(runs.slice().reverse(), function (benchmarkRun) {
                        return benchmarkRun.status === 'ENDED';
                    });
                    var benchmarkRunsHelper = new BenchmarkRunsHelper(benchmarkRuns);
                    $scope.aggregatedExecutionsMeasurementGraphsData = benchmarkRunsHelper.aggregatedExecutionsMeasurementGraphsData('lineChart', $filter);
                    $scope.benchmarkMeasurementGraphsData = benchmarkRunsHelper.benchmarkMeasurementGraphsData('lineChart', $filter);
                });
        }])
        .controller('BenchmarkRunCtrl', ['$scope', '$routeParams', '$modal', 'BenchmarkService',
                                         'CartCompareService', function ($scope, $routeParams, $modal, BenchmarkService, CartCompareService) {
                BenchmarkService.loadBenchmarkRun($routeParams.uniqueName, $routeParams.benchmarkSequenceId)
                    .then(function (benchmarkRun) {
                        $scope.benchmarkRun = benchmarkRun;
                    });

                $scope.benchmarkFromParam = function (benchmarkRun) {
                    return benchmarkRun.started - 10 * 1000; // 10 seconds before start
                };

                $scope.benchmarkToParam = function (benchmarkRun) {
                    if (benchmarkRun.ended) {
                        return benchmarkRun.ended + 10 * 1000; // 10 seconds after end
                    }
                    return Date.now();
                };

                $scope.measurementUnit = function (measurementKey) {
                    return $scope.benchmarkRun.aggregatedMeasurements[measurementKey].unit;
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

                $scope.addToCompare = function (benchmarkRun) {
                    CartCompareService.add(benchmarkRun);
                };

                $scope.isAddedToCompare = function (benchmarkRun) {
                    return CartCompareService.contains(benchmarkRun);
                }
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
        }])
        .controller('CartCompareNavBarCtrl', ['$scope', '$location', 'CartCompareService', function ($scope, $location, CartCompareService) {
            $scope.compareCartSize = CartCompareService.size();
            $scope.benchmarkRuns = CartCompareService.getAll();

            $scope.$on('cart:changed', function () {
                $scope.compareCartSize = CartCompareService.size();
            });

            $scope.remove = function (benchmarkRun) {
                CartCompareService.remove(benchmarkRun);
            };

            $scope.compare = function () {
                var names = _.map($scope.benchmarkRuns, function (benchmarkRun) {
                    return benchmarkRun.uniqueName;
                }).join();
                var sequenceIds = _.map($scope.benchmarkRuns, function (benchmarkRun) {
                    return benchmarkRun.sequenceId;
                }).join();
                $location.path('compare/' + names + '/' + sequenceIds);
            }
        }])
        .controller('CompareCtrl', ['$scope', '$routeParams', '$filter', 'BenchmarkService', function ($scope, $routeParams, $filter, BenchmarkService) {
            $scope.benchmarkRuns = [];

            var benchmarkUniqueNames = $routeParams.benchmarkNames.split(',');
            var sequenceIds = $routeParams.benchmarkSequenceIds.split(',');
            if (benchmarkUniqueNames.length != sequenceIds.length) {
                throw new Error('Expected the same number of benchmark run names and sequence ids.');
            }
            for (var i in sequenceIds) {
                BenchmarkService.loadBenchmarkRun(benchmarkUniqueNames[i], sequenceIds[i])
                    .then(function (benchmarkRun) {
                        $scope.benchmarkRuns.push(benchmarkRun);
                        prepareChartData();
                    });
            }

            var prepareChartData = function () {
                if (sequenceIds.length != $scope.benchmarkRuns.length) {
                    return; // not all benchmarkRuns are loaded yet
                }

                // sort benchmarkRuns to match requested order
                var tmpBenchmarkRuns = $scope.benchmarkRuns;
                $scope.benchmarkRuns = [];
                for (var i in sequenceIds) {
                    $scope.benchmarkRuns.push(_.findWhere(tmpBenchmarkRuns, {uniqueName: benchmarkUniqueNames[i], sequenceId: sequenceIds[i]}))
                }

                var benchmarkRunsHelper = new BenchmarkRunsHelper($scope.benchmarkRuns);
                $scope.aggregatedExecutionsMeasurementGraphsData = benchmarkRunsHelper.aggregatedExecutionsMeasurementGraphsData('multiBarChart', $filter);
                $scope.benchmarkMeasurementGraphsData = benchmarkRunsHelper.benchmarkMeasurementGraphsData('multiBarChart', $filter);
            };
        }]);
}());