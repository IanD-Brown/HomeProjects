#include <cassert>
#include <iostream>
#include <algorithm>
#include <set>

#include "day10Solver.h"

using namespace std;

static bool s_part2 = false;



day10Solver::day10Solver(const string& testFile) : solver(testFile) {
}

void day10Solver::loadData(const string& line) {
	if ( !m_data.empty() ) {
		assert(m_data[ 0 ].size() == line.length());
	}
	vector<short> numbers;
	for ( const char c : line ) {
		numbers.push_back(c - '0');
	}
	m_data.push_back(numbers);
}

void day10Solver::clearData() {
	m_data.clear();
}

static short findPath(const vector<vector<short>>& data, size_t r, size_t c, short need, set<coordinate>& endPoints) {
	if ( data[ r ][ c ] != need ) {
		return 0;
	}
	if ( need == 9 ) {
		endPoints.insert(make_pair(r, c));
		return 1;
	}
		short count = 0;
		if ( r > 0 ) {
			count += findPath(data, r - 1, c, need + 1, endPoints);
		}

		if (r + 1 < data.size() ) {
			count += findPath(data, r + 1, c, need + 1, endPoints);
		}

		if ( c > 0 ) {
			count += findPath(data, r, c - 1, need + 1, endPoints);
		}

		if ( c + 1 < data[ r ].size() ) {
			count += findPath(data, r, c + 1, need + 1, endPoints);
		}

		return count;
	}

solveResult day10Solver::compute() {
	solveResult t = 0;

	for ( size_t r = 0; r < m_data.size(); ++r ) {
		for ( int c = 0; c < m_data[ r ].size(); ++c ) {
			if (m_data[ r ][ c ] == 0) {
				set<coordinate> ends;
				short p = findPath(m_data, r, c, 0, ends);
				t += s_part2 ? p : ends.size();
			}
		}
	}

	return t;
}

solveResult day10Solver::compute2() {
	s_part2 = true;
    return compute();
}

void day10Solver::loadTestData() {
	clearData();

	loadData("89010123");
	loadData("78121874");
	loadData("87430965");
	loadData("96549874");
	loadData("45678903");
	loadData("32019012");
	loadData("01329801");
	loadData("10456732");
}
