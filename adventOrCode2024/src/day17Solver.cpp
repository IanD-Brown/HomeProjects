#include "day17Solver.h"

#include <cassert>
#include <functional>
#include <iostream>
#include <set>

using namespace std;

day17Solver::Processor::Processor() : m_a(0), m_b(0), m_c(0) {}

void day17Solver::Processor::reset() {
	m_a = 0;
	m_b = 0;
	m_c = 0;
	m_program.clear();
}

solveResult day17Solver::Processor::value(int operand) const {
	switch (operand) { 
	case 0:
	case 1:
	case 2:
	case 3:
		return operand;
	case 4:
		return m_a;
	case 5:
		return m_b;
	case 6:
		return m_c;
	}

	return -1;
}

string day17Solver::Processor::run() {
	string result;
	for (solveResult pc = 0; pc < m_program.size(); pc += 2) {
		switch(m_program[pc]) {
		case 0:
			m_a >>= value(m_program[pc + 1]);
			break;
		case 1:
			m_b ^= value(m_program[pc + 1]);
			break;
		case 2:
			m_b = value(m_program[pc + 1]) % 8;
			break;
		case 3:
			if (m_a != 0) {
				pc = m_program[pc + 1];
				pc -= 2;
			}
			break;
		case 4:
			m_b ^= m_c;
			break;
		case 5:
			if (!result.empty()) {
				result += ',';
			}
			result += '0' + (value(m_program[pc + 1]) % 8);
			break;
		case 6:
			m_b = m_a >> value(m_program[pc + 1]);
			break;
		case 7:
			m_c = m_a >> value(m_program[pc + 1]);
			break;
		}
	}
	return result;
}

day17Solver::day17Solver(const string &testFile) :
	solver(testFile) {}

static void extractIf(const string &line, const string &start, function<void(solveResult)> store) {
	if (line.rfind(start, 0) == 0) {
		store(stoll(line.substr(start.length())));
	}
}

void day17Solver::loadData(const string &line) {
	extractIf(line, "Register A: ", [&](solveResult v) { m_processor.m_a = v; });
	extractIf(line, "Register B: ", [&](solveResult v) { m_processor.m_b = v; });
	extractIf(line, "Register C: ", [&](solveResult v) { m_processor.m_c = v; });

	if (line.rfind("Program: ", 0) == 0) {
		m_program = line.substr(9);
		m_processor.m_program = asVectorInt(m_program, ",");
	}
}

void day17Solver::clearData() { 
	m_processor.reset();
	m_program.clear();
}

static void getReverse(const vector<int> &program, 
	                          size_t digit, set<solveResult> prior, 
	                          function<string(solveResult)> process,
	                          set<solveResult> &destination) {
	int instruction(program[digit]);
	string requiredOutput;
	for (int i = digit; i < program.size(); ++i) {
		if (i != digit) {
			requiredOutput += ',';
		}
		requiredOutput += '0' + program[i];
	}
	set<solveResult> possibles;
	for (solveResult p : prior) {
		for (int i = 0; i < 8; ++i) {
			solveResult a(p << 3 | i);
			if (process(a) == requiredOutput) {
				possibles.insert(a);
			}
		}
	}
	if (digit > 0) {
		getReverse(program, digit - 1, possibles, process, destination);
	} else {
		destination.insert(possibles.cbegin(), possibles.end());
	}
}

string day17Solver::computeString() {
	if (m_part1) {
		return m_processor.run();
	}

	set<solveResult> seed({0LL});
	set<solveResult> destination;
	getReverse(
		m_processor.m_program, m_processor.m_program.size() - 1, seed,
		[&](solveResult a) {
			m_processor.m_a = a;
			m_processor.m_b = 0;
			m_processor.m_c = 0;
			return m_processor.run();
		},
		destination);
	return to_string(*destination.begin());
}

void day17Solver::loadTestData() {
	bool state(m_part1);
	clearData();

	m_part1 = true;
	loadData("Register A: 37221261688308");
	loadData("Register B: 0");
	loadData("Register C: 0");
	loadData("");
	loadData("Program: 2,4,1,2,7,5,4,1,1,3,5,5,0,3,3,0");

	string output(computeString());
	cout << output << endl;
	assert(output == "2,4,1,2,7,5,4,1,1,3,5,5,0,3,3,0");
	m_part1 = state;
	clearData();

	loadData("Register A: 729");
	loadData("Register B: 0");
	loadData("Register C: 0");
	loadData("");
	if (m_part1) {
		loadData("Program: 0,1,5,4,3,0");
	} else {
		loadData("Program: 0,3,5,4,3,0");
	}
}
