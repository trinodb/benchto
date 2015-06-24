/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.services', [])
        .factory('BenchmarkService', ['$http', '$q', function ($http, $q) {
            var sortMeasurements = function (measurements) {
                return _.sortBy(measurements, function (measurement) {
                    // we want duration measurement to go first in UI
                    if (measurement.name == 'duration') {
                        // special characters are before alphanumeric ascii codes
                        return '!';
                    }
                    return measurement.name;
                });
            };

            var postProcessBenchmarkRun = function (benchmarkRun) {
                benchmarkRun.executions = _.sortBy(benchmarkRun.executions, function (execution) {
                    var sequenceId = execution.sequenceId;
                    var length = sequenceId.length;
                    if (length == 1) {
                        return '00' + sequenceId
                    } else if (length == 2) {
                        return '0' + sequenceId
                    } else if (length == 3) {
                        return sequenceId;
                    } else {
                        throw 'Too long sequence to be well sorted: ' + sequenceId;
                    }
                });
                benchmarkRun.measurements = sortMeasurements(benchmarkRun.measurements);
                _.each(benchmarkRun.executions, function (execution) {
                    execution.measurements = sortMeasurements(execution.measurements);
                });

                // convert to angular date filter consumable format
                benchmarkRun.started = benchmarkRun.started * 1000;
                benchmarkRun.ended = benchmarkRun.ended * 1000;
            };

            return {
                loadBenchmarkRun: function (benchmarkName, benchmarkSequenceId) {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/' + benchmarkName + '/' + benchmarkSequenceId
                    }).then(function (response) {
                        var benchmarkRun = response.data;
                        postProcessBenchmarkRun(benchmarkRun);
                        deferredBenchmark.resolve(benchmarkRun);
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                },
                loadBenchmark: function (benchmarkName) {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/' + benchmarkName
                    }).then(function (response) {
                        var benchmark = response.data;
                        benchmark.runs.forEach(function (benchmarkRun) {
                            postProcessBenchmarkRun(benchmarkRun);
                        });
                        benchmark.runs.reverse();
                        deferredBenchmark.resolve(benchmark);
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                },
                loadLatestBenchmarkRuns: function () {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/latest'
                    }).then(function (response) {
                        var benchmarkRuns = response.data;
                        benchmarkRuns.forEach(function (benchmarkRun) {
                            postProcessBenchmarkRun(benchmarkRun);
                        });
                        deferredBenchmark.resolve(benchmarkRuns);
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                }
            };
        }])
        .factory('EnvironmentService', ['$http', '$q', function ($http, $q) {
            return {
                loadEnvironment: function (environmentName) {
                    var deferredEnvironment = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/environment/' + environmentName
                    }).then(function (response) {
                        deferredEnvironment.resolve(response.data);
                    }, function (reason) {
                        deferredEnvironment.reject(reason);
                    });
                    return deferredEnvironment.promise;
                }
            };
        }]);
}());