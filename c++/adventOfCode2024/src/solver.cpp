#include "solver.h"
#include <cassert>
#include <fstream>
#include <sstream>
#include <iostream>

using namespace std;

solver::solver(const string& testFile) : m_testFile {testFile}, m_part1(true), m_test(false) {
};

void solver::loadFromFile() {
	m_test = false;
	clearData();
    loadFromFile(m_testFile);
}

void solver::loadFromFile(const string& file) {
    ifstream myfile;
    myfile.open(file);
    string line;
    while (getline(myfile, line)) {
        if (!line.empty()) {
            loadData(line);
        }
    }
}

solveResult solver::compute2() {
    m_part1 = false;
	return compute();
}

void solver::assertEquals(size_t actual, size_t expected, const std::string& message) {
	if ( actual != expected ) {
		cout << "actual " << actual << " expected " << expected << ' ' << message << endl;
		assert(actual == expected);
	}
	clearData();
}

vector<int> solver::asVectorInt(const string& line, const string& delimiter) {
    size_t pos_start = 0, pos_end, delim_len = delimiter.length();
    vector<int> values;

    while ((pos_end = line.find(delimiter, pos_start)) != string::npos) {
        values.push_back(stoi(line.substr(pos_start, pos_end - pos_start)));
        pos_start = pos_end + delim_len;
    }
    values.push_back(stoi(line.substr(pos_start)));

    return values;
}

string solver::getDay() {
  return m_testFile.substr(0, m_testFile.find('\\') + 1);
}