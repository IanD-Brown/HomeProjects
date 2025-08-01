#include "day23Solver.h"

#include <cstdint>
#include <iostream>
#include <map>
#include <solver.h>
#include <string>

using namespace std;

day23Solver::day23Solver(const string &testFile) : solver(testFile) {}

void day23Solver::loadData(const string &line) { 
    m_data.push_back(line); 
}

void day23Solver::clearData() {
    m_data.clear(); 
}

solveResult day23Solver::compute() {
  solveResult r(0);

  return r;
}

void day23Solver::loadTestData() {
  m_test = true;

  clearData();

  loadData("kh-tc");
  loadData("qp-kh");
  loadData("de-cg");
  loadData("ka-co");
  loadData("yn-aq");
  loadData("qp-ub");
  loadData("cg-tb");
  loadData("vc-aq");
  loadData("tb-ka");
  loadData("wh-tc");
  loadData("yn-cg");
  loadData("kh-ub");
  loadData("ta-co");
  loadData("de-co");
  loadData("tc-td");
  loadData("tb-wq");
  loadData("wh-td");
  loadData("ta-ka");
  loadData("td-qp");
  loadData("aq-cg");
  loadData("wq-ub");
  loadData("ub-vc");
  loadData("de-ta");
  loadData("wq-aq");
  loadData("wq-vc");
  loadData("wh-yn");
  loadData("ka-de");
  loadData("kh-ta");
  loadData("co-tc");
  loadData("wh-qp");
  loadData("tb-vc");
  loadData("td-yn");
}