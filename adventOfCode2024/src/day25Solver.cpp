#include "day25Solver.h"

#include <cstdlib>
#include <iostream>
#include <map>
#include <regex>
#include <set>
#include <solver.h>
#include <string>
#include <vector>

using namespace std;

day25Solver::day25Solver(const string &testFile) : solver(testFile) {}

void day25Solver::loadData(const string &line) {
  if (!line.empty()) {
    m_data.push_back(line);
  }
}

void day25Solver::clearData() {
    m_data.clear(); 
}

solveResult day25Solver::compute() {
  return 0;
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