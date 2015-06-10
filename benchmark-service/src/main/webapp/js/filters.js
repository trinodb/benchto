/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.filters', [])
        .filter('unit', ['numberFilter', function (numberFilter) {
            return function (value, unit) {
                var outputValueText = '';
                var outputUnitText = '';

                if (unit === 'MILLISECONDS') {
                    outputUnitText = 'ms';
                    if ((value / 1000) > 1) {
                        outputUnitText = 's';
                        value /= 1000;
                    }
                    if ((value / 60) > 1) {
                        outputUnitText = 'm';
                        value /= 60;
                    }
                    if ((value / 60) > 1) {
                        outputUnitText = 'h';
                        value /= 60;
                    }

                    outputValueText += numberFilter(value, 2);
                }
                else if (unit === 'BYTES') {
                    outputUnitText = 'B';
                    if ((value / 1000) > 1) {
                        outputUnitText = 'KB';
                        value /= 1000;
                    }
                    if ((value / 1000) > 1) {
                        outputUnitText = 'MB';
                        value /= 1000;
                    }
                    if ((value / 1000) > 1) {
                        outputUnitText = 'GB';
                        value /= 1000;
                    }

                    outputValueText += numberFilter(value, 2);
                }
                else if (unit === 'PERCENT') {
                    outputValueText += numberFilter(value, 2);
                    outputUnitText = '%';
                }
                else {
                    outputValueText += numberFilter(value, 2);
                    outputUnitText = unit;
                }

                return outputValueText + ' ' + outputUnitText;
            };
        }]);
}());