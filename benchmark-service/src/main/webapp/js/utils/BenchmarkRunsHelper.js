/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */


function BenchmarkRunsHelper(benchmarkRuns) {
    this.benchmarkRuns = benchmarkRuns;
}

BenchmarkRunsHelper.prototype.aggregatedExecutionsMeasurementKeys = function () {
    return _.uniq(_.flatten(_.map(this.benchmarkRuns,
        function (benchmarkRun) {
            return _.allKeys(benchmarkRun.aggregatedMeasurements);
        }
    )));
};

BenchmarkRunsHelper.prototype.aggregatedExecutionsMeasurementUnit = function (measurementKey) {
    for (var i = 0; i < this.benchmarkRuns.length; ++i) {
        var aggregatedMeasurement = this.benchmarkRuns[i].aggregatedMeasurements[measurementKey];
        if (aggregatedMeasurement) {
            return aggregatedMeasurement.unit;
        }
    }
    throw new Error("Could not find unit for measurement key: " + measurementKey);
};

BenchmarkRunsHelper.prototype.extractAggregatedExecutionsAggregatedMeasurements = function (measurementKey) {
    return _.map(this.benchmarkRuns, function (benchmarkRun) {
        return benchmarkRun.aggregatedMeasurements[measurementKey];
    });
};

// benchmark measurements graph data
BenchmarkRunsHelper.prototype.benchmarkMeasurementKeys = function () {
    return _.chain(this.benchmarkRuns)
        .map(function (benchmarkRun) { return _.pluck(benchmarkRun.measurements, 'name'); })
        .flatten()
        .uniq()
        .value();
};

BenchmarkRunsHelper.prototype.benchmarkMeasurementUnit = function (measurementKey) {
    for (var i = 0; i < this.benchmarkRuns.length; ++i) {
        var measurement = _.findWhere(this.benchmarkRuns[i].measurements, {name: measurementKey});
        if (measurement) {
            return measurement.unit;
        }
    }
    throw new Error("Could not find unit for measurement key: " + measurementKey);
};

BenchmarkRunsHelper.prototype.extractBenchmarkMeasurements = function (measurementKey) {
    return _.map(this.benchmarkRuns, function (benchmarkRun) {
        return _.findWhere(benchmarkRun.measurements, {name: measurementKey});
    });
};
