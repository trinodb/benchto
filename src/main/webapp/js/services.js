/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.service', [])
        .factory('BenchmarkService', ['$http', '$q', function ($http, $q) {
            return {
                loadBenchmark: function (benchmarkName, benchmarkSequenceId) {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/' + benchmarkName + '/' + benchmarkSequenceId
                    }).then(function (response) {
                        var benchmark = response.data;
                        benchmark.executions = _.sortBy(benchmark.executions, 'sequenceId');
                        benchmark.measurements = _.sortBy(benchmark.measurements, 'name');
                        deferredBenchmark.resolve(benchmark);
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                },
                loadBenchmarkRuns: function (benchmarkName, page, size) {
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
                        var benchmarks = response.data;
                        benchmarks.forEach(function (benchmark) {
                            benchmark.executions = _.sortBy(benchmark.executions, 'sequenceId');
                            benchmark.measurements = _.sortBy(benchmark.measurements, 'name');
                            deferredBenchmark.resolve(benchmarks);
                        });
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                },
                loadLatestBenchmarks: function (page, size) {
                    var deferredBenchmark = $q.defer();
                    $http({
                        method: 'GET',
                        url: '/v1/benchmark/latest',
                        params: {
                            page: page,
                            size: size
                        }
                    }).then(function (response) {
                        var benchmarks = response.data;
                        benchmarks.forEach(function (benchmark) {
                            benchmark.executions = _.sortBy(benchmark.executions, 'sequenceId');
                            benchmark.measurements = _.sortBy(benchmark.measurements, 'name');
                            deferredBenchmark.resolve(benchmarks);
                        });
                    }, function (reason) {
                        deferredBenchmark.reject(reason);
                    });
                    return deferredBenchmark.promise;
                }
            };
        }]);
}());