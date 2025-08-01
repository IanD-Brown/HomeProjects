#include "day22Solver.h"

#include <cstdint>
#include <iostream>
#include <map>
#include <solver.h>
#include <string>

using namespace std;

day22Solver::day22Solver(const string &testFile) : solver(testFile) {}

void day22Solver::loadData(const string &line) { 
    m_data.push_back(line); 
}

void day22Solver::clearData() {
    m_data.clear(); 
}

solveResult day22Solver::compute() {
  solveResult r(0);
  map<int32_t, int> totalValues;
  int limit(2000);

  for (const auto &l : m_data) {
    solveResult s(stoll(l));
    char p(0);
    int32_t k(0);
    map<int32_t, int> values;

    for (int i = 0; i < limit; ++i) {
      if (!m_part1) {
        char lastDigit(s % 10);
        k = (k << 8) | ((lastDigit - p) & 0xff);
        p = lastDigit;
        if (i > 2 && values.find(k) == values.cend()) {
          values[k] = lastDigit;
        }
      }
      s = ((s * 64LL) ^ s) % 16777216LL;
      s = ((s / 32LL) ^ s) % 16777216LL;
      s = ((s * 2048LL) ^ s) % 16777216LL;
    }
    r += s;
    for (const auto &it : values) {
      auto fnd(totalValues.find(it.first));

      if (fnd != totalValues.end()) {
        totalValues[it.first] = it.second + fnd->second;
      } else {
        totalValues[it.first] = it.second;
      }
    }
  }
  if (!m_part1) {
    int m(0);
    for (const auto &it : totalValues) {
      if (it.second > m) {
        m = it.second;
      }
    }
    return m;
  }

  return r;
}

void day22Solver::loadTestData() {
  m_test = true;

  clearData();

  if (m_part1) {
    loadData("1");
    loadData("10");
    loadData("100");
  } else {
    loadData("1");
    loadData("2");
    loadData("3");
  }
  loadData("2024");
}