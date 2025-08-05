#include "day24Solver.h"

#include <iostream>
#include <map>
#include <solver.h>
#include <string>
#include <algorithm>
#include <iterator>
#include <set>
#include <vector>
#include <cstdint>

using namespace std;


day24Solver::day24Solver(const string &testFile) : solver(testFile) {}

void day24Solver::loadData(const string &line) { 
    m_data.push_back(line); 
}

void day24Solver::clearData() {
    m_data.clear(); 
}

solveResult day24Solver::compute() {
  solveResult r(0);

  return r;
}

void day24Solver::loadTestData() {
  m_test = true;

  clearData();

  loadData("x00: 1");
  loadData("x01: 0");
  loadData("x02: 1");
  loadData("x03: 1");
  loadData("x04: 0");
  loadData("y00: 1");
  loadData("y01: 1");
  loadData("y02: 1");
  loadData("y03: 1");
  loadData("y04: 1");
  loadData("");
  loadData("ntg XOR fgs -> mjb");
  loadData("y02 OR x01 -> tnw");
  loadData("kwq OR kpj -> z05");
  loadData("x00 OR x03 -> fst");
  loadData("tgd XOR rvg -> z01");
  loadData("vdt OR tnw -> bfw");
  loadData("bfw AND frj -> z10");
  loadData("ffh OR nrd -> bqk");
  loadData("y00 AND y03 -> djm");
  loadData("y03 OR y00 -> psh");
  loadData("bqk OR frj -> z08");
  loadData("tnw OR fst -> frj");
  loadData("gnj AND tgd -> z11");
  loadData("bfw XOR mjb -> z00");
  loadData("x03 OR x00 -> vdt");
  loadData("gnj AND wpb -> z02");
  loadData("x04 AND y00 -> kjc");
  loadData("djm OR pbm -> qhw");
  loadData("nrd AND vdt -> hwm");
  loadData("kjc AND fst -> rvg");
  loadData("y04 OR y02 -> fgs");
  loadData("y01 AND x02 -> pbm");
  loadData("ntg OR kjc -> kwq");
  loadData("psh XOR fgs -> tgd");
  loadData("qhw XOR tgd -> z09");
  loadData("pbm OR djm -> kpj");
  loadData("x03 XOR y03 -> ffh");
  loadData("x00 XOR y04 -> ntg");
  loadData("bfw OR bqk -> z06");
  loadData("nrd XOR fgs -> wpb");
  loadData("frj XOR qhw -> z04");
  loadData("bqk OR frj -> z07");
  loadData("y03 OR x01 -> nrd");
  loadData("hwm AND bqk -> z03");
  loadData("tgd XOR rvg -> z12");
  loadData("tnw OR pbm -> gnj");
  loadData("");
}