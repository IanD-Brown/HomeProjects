#include "day25Solver.h"

#include <iostream>
#include <solver.h>
#include <string>
#include <vector>
#include <fstream>

using namespace std;

ostream &operator<<(ostream &os, const vector<size_t> &vector) {
  // Printing all the elements using <<
  for (const auto &i : vector) {
    os << i << ", ";
  }
  return os;
}


day25Solver::day25Solver(const string &testFile) : solver(testFile), m_state(EMPTY), m_height(0) {}

void day25Solver::loadFromFile(const string &file) {
  ifstream myfile;
  myfile.open(file);
  string line;
  while (getline(myfile, line)) {
    loadData(line);
  }
}

void day25Solver::loadData(const string &line) {
  if (line.empty()) {
    switch (m_state) {
    case LOCK:
      m_locks.push_back(m_working);
      break;
    case KEY:
      m_keys.push_back(m_working);
      break;
    }
    m_working.clear();
    m_state = EMPTY;
    return;
  }
  if (m_state == EMPTY) {
    m_state = line[0] == '#' ? LOCK : KEY;
    m_working.insert(m_working.end(), line.size(), 0);
    return;
  }

  if (m_locks.empty() && m_keys.empty()) {
    ++m_height;
  }
  for (size_t i = 0; i < line.size(); ++i) {
    if (line[i] == '#') {
      m_working[i] = m_working[i] + 1;
    }
  }
}

void day25Solver::clearData() { 
  m_locks.clear();
  m_keys.clear();
  m_height = 0;
}

solveResult day25Solver::compute() {
  if (m_state != EMPTY) {
    switch (m_state) {
    case LOCK:
      m_locks.push_back(m_working);
      break;
    case KEY:
      m_keys.push_back(m_working);
      break;
    }
    m_working.clear();
    m_state = EMPTY;
  }
  solveResult count(0);
  for (const auto &i : m_locks) {
    for (const auto &j : m_keys) {
      bool fit(true);
      for (size_t k(0); k < j.size(); ++k) {
        if (i[k] + j[k] > m_height) {
          fit = false;

          break;
        }
      }
      if (fit) {
        ++count;
      }
    }
  }
  return count;
}

void day25Solver::loadTestData() {
  m_test = true;

  clearData();

loadData("#####");
  loadData(".####");
  loadData(".####");
  loadData(".####");
  loadData(".#.#.");
  loadData(".#...");
  loadData(".....");
  loadData("");
  loadData("#####");
  loadData("##.##");
  loadData(".#.##");
  loadData("...##");
  loadData("...#.");
  loadData("...#.");
  loadData(".....");
  loadData("");
  loadData(".....");
  loadData("#....");
  loadData("#....");
  loadData("#...#");
  loadData("#.#.#");
  loadData("#.###");
  loadData("#####");
  loadData("");
  loadData(".....");
  loadData(".....");
  loadData("#.#..");
  loadData("###..");
  loadData("###.#");
  loadData("###.#");
  loadData("#####");
  loadData("");
  loadData(".....");
  loadData(".....");
  loadData(".....");
  loadData("#....");
  loadData("#.#..");
  loadData("#.#.#");
  loadData("#####");
  loadData("");
}