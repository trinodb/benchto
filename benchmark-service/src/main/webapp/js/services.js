/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.service', [])
        .factory('BenchmarkService', ['$http', '$q', function ($http, $q) {
            return {
                loadBenchmarkRun: function (benchmarkName, benchmarkSequenceId) {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/' + benchmarkName + '/' + benchmarkSequenceId
                    }).then(function (response) {
                        var benchmarkRun = response.data;
                        benchmarkRun.executions = _.sortBy(benchmarkRun.executions, 'sequenceId');
                        benchmarkRun.measurements = _.sortBy(benchmarkRun.measurements, 'name');
                        deferredBenchmark.resolve(benchmarkRun);
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                },
                loadBenchmark: function (benchmarkName, page, size) {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/' + benchmarkName,
                        params: {
                            page: page,
                            size: size,
                            sort: 'sequenceId,desc'
                        }
                    }).then(function (response) {
                        var benchmark = response.data;
                        benchmark.runs.forEach(function (benchmarkRun) {
                            benchmarkRun.executions = _.sortBy(benchmarkRun.executions, 'sequenceId');
                            benchmarkRun.measurements = _.sortBy(benchmarkRun.measurements, 'name');
                        });
                        benchmark.runs.reverse();
                        deferredBenchmark.resolve(benchmark);
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                },
                loadLatestBenchmarkRuns: function (page, size) {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/latest',
                        params: {
                            page: page,
                            size: size
                        }
                    }).then(function (response) {
                        var benchmarkRuns = response.data;
                        benchmarkRuns.forEach(function (benchmarkRun) {
                            benchmarkRun.executions = _.sortBy(benchmarkRun.executions, 'sequenceId');
                            benchmarkRun.measurements = _.sortBy(benchmarkRun.measurements, 'name');
                            deferredBenchmark.resolve(benchmarkRuns);
                        });
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                }
            };
        }]);
}());