#include <cassert>
#include <iostream>
#include <algorithm>

#include "day8Solver.h"

using namespace std;

static bool s_part2 = false;

day8Solver::day8Solver(const string& testFile) : solver(testFile) {
}

void day8Solver::loadData(const string& line) {
	if ( m_grid.m_colCount > 0 ) {
		assert(m_grid.m_colCount == line.size());
	} else {
		m_grid.m_colCount = line.size();
	}
	for ( size_t c = 0; c < line.size(); ++c ) {
		char v = line[ c ];
		if ( v != '.' ) {
			m_grid.addNode(v, c);
		}
	}
	++m_grid.m_rowCount;
}

void day8Solver::clearData() {
  m_grid.clear();
}

/*
* Say a is at 3/7
*     b might be at 5/4
* row will always be larger
* need to set 1/10 and 7/1
* i.e.
* ra - (rb - ra) / ca - (cb - ca)
* rb + (rb - ra) / cb + (cb - ca)
*/
static void calcLine(const vector<day8Solver::node>& nodes, size_t index, day8Solver::grid& p_grid) {
	const day8Solver::node& a = nodes[ index ];
	for ( size_t i = index + 1; i < nodes.size(); ++i ) {
		const day8Solver::node& b = nodes[ i ];
		int rDelta = b.first - a.first;
		int cDelta = b.second - a.second;

		if ( !s_part2 ) {
			p_grid.inGrid(a.first - rDelta, a.second - cDelta);

			p_grid.inGrid(b.first + rDelta, b.second + cDelta);
		} 
		else {
			int newR = a.first - rDelta;
			int newC = a.second - cDelta;

			while ( p_grid.inGrid(newR, newC) ) {
				newR -= rDelta;
				newC -= cDelta;
			}
			newR = b.first + rDelta;
			newC = b.second + cDelta;
			while ( p_grid.inGrid(newR, newC) ) {
				newR += rDelta;
				newC += cDelta;
			}

		}
	}
	if ( index < nodes.size() - 1 ) {
		calcLine(nodes, index + 1, p_grid);
	}
}

solveResult day8Solver::compute() {
	solveResult t = 0;

	for ( const auto& nodes : m_grid.m_data ) {
		calcLine(nodes.second, 0, m_grid);
		if ( s_part2 && nodes.second.size() > 1 ) {
			m_grid.m_visited.insert(nodes.second.begin(), nodes.second.end());
		}
	}

	t += m_grid.m_visited.size();

	return t;
}

solveResult day8Solver::compute2() {
	s_part2 = true;
    return compute();
}

void day8Solver::loadTestData() {
  clearData();

    loadData("............");
    loadData("........0...");
	loadData(".....0......");
	loadData(".......0....");
	loadData("....0.......");
	loadData("......A.....");
	loadData("............");
	loadData("............");
	loadData("........A...");
	loadData(".........A..");
	loadData("............");
	loadData("............");

}
