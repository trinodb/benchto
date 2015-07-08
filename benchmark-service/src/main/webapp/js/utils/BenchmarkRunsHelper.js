/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */


function BenchmarkRunsHelper(benchmarkRuns) {
    this.benchmarkRuns = benchmarkRuns;
}

BenchmarkRunsHelper.prototype.aggregatedExecutionsMeasurementKeys = function() {
    return _.uniq(_.flatten(_.map(this.benchmarkRuns,
        function (benchmarkRun) {
            return _.allKeys(benchmarkRun.aggregatedMeasurements);
        }
    )));
}

BenchmarkRunsHelper.prototype.aggregatedExecutionsMeasurementUnit = function (measurementKey) {
    return this.benchmarkRuns[0].aggregatedMeasurements[measurementKey].unit;
};

BenchmarkRunsHelper.prototype.extractAggregatedExecutionsAggregatedMeasurements = function (measurementKey) {
    return _.map(this.benchmarkRuns, function (benchmarkRun) {
        return benchmarkRun.aggregatedMeasurements[measurementKey];
    });
};

// benchmark measurements graph data
BenchmarkRunsHelper.prototype.benchmarkMeasurementKeys = function() {
    return _.uniq(_.flatten(_.map(this.benchmarkRuns,
        function (benchmarkRun) {
            return _.pluck(benchmarkRun.measurements, 'name');
        }
    )));
}

BenchmarkRunsHelper.prototype.findBenchmarkMeasurement = function(benchmarkRun, measurementKey) {
    return _.findWhere(benchmarkRun.measurements, { name: measurementKey });
};

BenchmarkRunsHelper.prototype.benchmarkMeasurementUnit = function (measurementKey) {
    return findBenchmarkMeasurement(this.benchmarkRuns[0], measurementKey).unit;
};

BenchmarkRunsHelper.prototype.extractBenchmarkMeasurements = function (measurementKey) {
    return _.map(this.benchmarkRuns, function (benchmarkRun) {
        return findBenchmarkMeasurement(benchmarkRun, measurementKey);
    });
};
