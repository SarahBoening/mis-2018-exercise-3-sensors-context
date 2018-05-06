
Thresholds via Location.getSpeed:
- jogging: 1 - 13 km/h (source: https://www.iamlivingit.com/running/average-human-running-speed)
- riding a bike: 14–25 km/h (source: https://en.wikipedia.org/wiki/Bicycle_performance#Energy_efficiency)
- no movement: <1 km/h
- bus/car: > 25 km/h 

Determining frequency changes:
- after FFT search for highest Frequency
- if this is significantly bigger/smaller then the old maxFreq => check with Location.getSpeed => change music player state if necessary


