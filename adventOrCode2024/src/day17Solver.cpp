#include "day17Solver.h"

#include <cassert>
#include <functional>
#include <iostream>


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

struct ReverseProcessor : day17Solver::Processor {

	ReverseProcessor(const day17Solver::Processor &src) {
		m_a = 0;
		m_b = src.m_b;
		m_c = src.m_c;
		m_program = src.m_program;
	}

	string run();
};

string ReverseProcessor::run() {
	string result;
	solveResult t(0);
	for (int p : m_program) {
		t = (t << 3) | p;
	}
	cout << t << endl;
	result = to_string(t);
	for (solveResult pc = m_program.size() - 2; pc >= 0; pc -= 2) {
		switch (m_program[pc]) {
		case 0:
			cout << "adv " << value(m_program[pc + 1]) << endl;
			m_a *= pow(2, value(m_program[pc + 1]));
			break;
		case 1:
			m_b ^= value(m_program[pc + 1]);
			break;
		case 2:
			cout << "bxl " << value(m_program[pc + 1]) % 8 << endl;
			break;
		case 3:
			if (m_a != 666) {
				m_a = 666;
				cout << "jnz " << m_program[pc + 1] << endl;
				pc = m_program.size() - m_program[pc + 1];
			}
			break;
		case 4:
			cout << "bxc " << m_c << endl;
//			m_b ^= m_c;
			break;
		case 5:
			cout << "out " << (value(m_program[pc + 1]) % 8) << endl;
			//if (!result.empty()) {
			//	result += ',';
			//}
			//result += '0' + (value(m_program[pc + 1]) % 8);
			break;
		case 6:
			cout << "bdv " << value(m_program[pc + 1]) << endl;
			//m_b = m_a / pow(2, value(m_program[pc + 1]));
			break;
		case 7:
			cout << "cdv " << value(m_program[pc + 1]) << endl;
			//m_c = m_a / pow(2, value(m_program[pc + 1]));
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

string day17Solver::computeString() {
	if (m_part1) {
		return m_processor.run();
	}
	ReverseProcessor reverse(m_processor);
	return reverse.run();
	//// brute force???
	//solveResult a(0);
	//solveResult b(m_processor.m_b);
	//solveResult c(m_processor.m_c);
	//for (;;) {
	//	if (m_processor.run() == m_program) {
	//		return to_string(m_processor.m_a);
	//	}
	//	++a;
	//	m_processor.m_a = a;
	//	m_processor.m_b = b;
	//	m_processor.m_c = c;
	//	if ((a % 100000) == 0) {
	//		cout << a << endl;
	//	}
	//}
}

void day17Solver::loadTestData() {
	bool state(m_part1);
	clearData();

	loadData("Register A: 729");
	loadData("Register B: 0");
	loadData("Register C: 0");
	loadData("");
	loadData("Program: 0,1,5,4,3,0");
}
