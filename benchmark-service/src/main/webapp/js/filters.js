/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
(function () {
    'use strict';

    angular.module('benchmarkServiceUI.filters', [])
        .filter('duration', function(){
            return function(mills){
                var seconds = 0, minutes = 0, hours =0;
                var seconds = mills/1000;
                if(seconds > 0){
                    minutes = Math.floor(seconds/60);
                    seconds %= 60;
                }
                if(minutes > 0){
                    hours = Math.floor(minutes/60);
                    minutes %= 60;
                }
                var result = "";
                if(hours > 0){
                    result += (hours+"h")
                }
                if(minutes > 0){
                    result += (minutes+"m")
                }
                result+=(seconds.toFixed(3)+"s")
                return result;
            }
        })
        .filter('unit', ['numberFilter', 'durationFilter', function (numberFilter, durationFilter) {
            return function (value, unit) {
                var outputValueText = '';
                var outputUnitText = '';

                if (unit === 'MILLISECONDS') {
                    outputValueText = durationFilter(value)
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