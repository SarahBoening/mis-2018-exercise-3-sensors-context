
Thresholds via Location.getSpeed:
- jogging: 1 - 13 km/h (source: https://www.iamlivingit.com/running/average-human-running-speed)
- riding a bike: 14–25 km/h (source: https://en.wikipedia.org/wiki/Bicycle_performance#Energy_efficiency)
- no movement: <1 km/h
- bus/car: > 25 km/h 

Thresholds for FFT data:
- by using the app from 3a, one could see that when the device is not moving the peek of the FFT is always below 20, mostly in between 0,2 and 10. Since the device can be moved simply by adjusting one's seeting position (meaning a person is not walking) a walking movement should give a FFT peek at above 20.

Note: if no location permission is granted, only the fft threshold is used

Determining frequency changes:
- after FFT search for highest frequency
- if this is significantly bigger/smaller then the old maxFreq => check with Location.getSpeed => change music player state if necessary


