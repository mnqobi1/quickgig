// tests/hustle.unit.test.js
const { pointsForCompletion } = require('../src/hustle');

describe('pointsForCompletion', () => {
  it('gives the base amount for a 0-star, late completion', () => {
    expect(pointsForCompletion({ stars: 0, onTime: false })).toBe(10);
  });
  it('gives full points for a 5-star, on-time completion', () => {
    expect(pointsForCompletion({ stars: 5, onTime: true })).toBe(25);
  });
  it('scales the rating bonus proportionally', () => {
    expect(pointsForCompletion({ stars: 2.5, onTime: false })).toBe(15);
  });
  it('clamps out-of-range star values instead of erroring', () => {
    expect(pointsForCompletion({ stars: 8, onTime: false })).toBe(20);
    expect(pointsForCompletion({ stars: -3, onTime: false })).toBe(10);
  });
});
